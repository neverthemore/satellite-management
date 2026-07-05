# Satellite Management System — нагрузочное тестирование

Этот раздел репозитория содержит нагрузочный тест ключевого пользовательского
сценария сервиса `satellite-spring` (REST API управления спутниковыми
группировками), выполненный в рамках курса по системному дизайну.

- Инструмент: [k6](https://k6.io)
- Отчётность: [Allure Report](https://allurereport.org) (генерируется из
  результатов k6 отдельным адаптером) + нативный HTML-отчёт k6 как дублирующий
  быстрый способ посмотреть агрегированные цифры
- Файлы теста: [`load-tests/`](./load-tests)

---

## Пользовательский сценарий

**«Оператор Центра управления полётами разворачивает новую спутниковую
группировку и следит за её статусом»**

Сценарий имитирует законченный рабочий цикл оператора: от создания
группировки до контроля телеметрии перед завершением смены. Каждый
виртуальный пользователь k6 проходит все 7 шагов последовательно, работая
со своей собственной, уникальной группировкой (имя строится из id
виртуального пользователя, номера итерации и таймстемпа) — это исключает
конфликты между параллельными пользователями на уникальности имени
(API отвечает `422`, если группировка с таким именем уже существует) и
реалистично имитирует то, что разные операторы управляют разными
группировками одновременно.

| # | Шаг | Метод и путь | Тип |
|---|-----|--------------|-----|
| 1 | Создать группировку | `POST /api/constellations?name=...` | запись |
| 2 | Добавить спутники (связь + съёмка) | `POST /api/add-satellites` | запись |
| 3 | Выполнить миссию (активация + отправка данных/снимки) | `POST /api/missions` | запись |
| 4 | Проверить статус своей группировки | `GET /api/constellations/{name}/report` | чтение |
| 5 | Посмотреть все активные спутники в системе | `GET /api/satellites/active` | чтение |
| 6 | Вывести спутник из эксплуатации | `DELETE /api/constellations/{name}/satellites/{sat}` | запись |
| 7 | Проверить телеметрию перед завершением смены | `GET /api/telemetry` | чтение |

Сценарий целиком реализован в [`load-tests/k6/scenario.js`](./load-tests/k6/scenario.js).

---

## Профиль нагрузки

| Параметр | Значение |
|---|---|
| Исполнитель (executor) | `ramping-vus` — разгон/удержание/спад |
| Пиковое число виртуальных пользователей | 30 по умолчанию (регулируется `VUS_MAX`) |
| Разгон | 0 → пик за 20с |
| **Удержание пика** | **60 секунд непрерывно** |
| Спад | пик → половина за 15с, далее → 0 за 10с |
| Суммарная длительность прогона | ~105 секунд |

Порог (threshold) считается пройденным, если:
- 95-й перцентиль длительности запроса < 1.5с
- Доля HTTP-ошибок < 1%
- Доля успешно завершённых итераций сценария > 95%

Число виртуальных пользователей по умолчанию (30) подобрано под комфортный
локальный запуск с Postgres + Kafka в Docker на обычном ноутбуке. При более
мощном железе увеличьте через переменную окружения `VUS_MAX` (см. ниже),
не редактируя сам скрипт.

---

## Структура файлов теста

```
load-tests/
├── k6/
│   └── scenario.js          — сам сценарий нагрузочного теста
├── allure-adapter/
│   └── convert.js           — конвертер JSON-вывода k6 в формат allure-results
├── run.sh                   — скрипт полного цикла для Linux/macOS/Git Bash/WSL
├── run.ps1                  — тот же скрипт для нативного Windows PowerShell
├── results/                 — (создаётся при запуске) сырые результаты k6, HTML-отчёт k6
├── allure-results/          — (создаётся при запуске) промежуточные *-result.json для Allure
└── allure-report/           — (создаётся при запуске) финальный сгенерированный Allure-отчёт
```

`results/`, `allure-results/` и `allure-report/` — генерируемые артефакты,
не хранятся в git (см. `.gitignore`).

---

## Инструкция по запуску

### 1. Предварительные требования

- Java 17 (или скорректируйте `sourceCompatibility` в `build.gradle.kts` под свою версию)
- Docker Desktop (для Postgres и Kafka)
- [k6](https://grafana.com/docs/k6/latest/set-up/install-k6/) — нагрузочный тест.
  На Windows проще всего: `winget install k6 --source winget`
- Node.js 16+ — для конвертера результатов в Allure (использует только
  встроенный модуль `crypto`, внешних зависимостей не требует)
- [Allure commandline](https://allurereport.org/docs/install/) — генерация отчёта
  (либо ставится глобально, либо вызывается через `npx` без установки —
  оба скрипта, `run.sh` и `run.ps1`, умеют и так, и так)
- На Windows скрипт `run.sh` требует Git Bash или WSL; альтернатива без
  них — нативный `run.ps1` (см. шаг 4)

### 2. Запуск инфраструктуры (Postgres + Kafka)

```bash
docker compose up postgres kafka
```

> Полный `docker compose up --build` пока не работает: сервис `satellite-spring`
> в `docker-compose.yml` ссылается на контекст `./satellite-spring`, в котором
> сейчас нет исходного кода (структура репозитория готовится к следующему
> разделению на отдельные модули). Именно поэтому здесь запускается только
> инфраструктура (Postgres, Kafka), а само приложение — шагом ниже, через Gradle.

Дождитесь, пока оба контейнера перейдут в статус `healthy` (в логах будет видно).

### 3. Запуск приложения

```bash
./gradlew generateProto   # обязательный первый шаг — генерирует Java-код из .proto
./gradlew bootRun
```

Успешный запуск оканчивается строкой в логе:

```
Started Main in ... seconds
```

и демонстрационным сценарием из `Main.java`: создаются группировки
«Орбита-1»/«Орбита-2», активируются спутники, выполняются миссии. Проверить
вручную: `http://localhost:8080/swagger-ui.html`.

<details>
<summary>Если сборка падает на этапе компиляции или запуска — разверните для списка известных фиксов</summary>

При работе над этим заданием были обнаружены и исправлены несколько
несовместимостей в коде текущей ветки (`satellite-management-11`),
не связанных с настройками окружения:

1. **`net.devh.boot.grpc.server.service does not exist`** — в
   `build.gradle.kts` отсутствовала зависимость `grpc-server-spring-boot-starter`
   (был только клиентский стартер). Добавлено:
   ```kotlin
   implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
   ```

2. **`Main class name has not been configured`** — в одной директории `src/`
   оказались три `@SpringBootApplication`-класса (`seminars.Main`,
   `telemetry.TelemetryApplication`, `scheduler.SchedulerApplication`).
   Явно указан главный класс:
   ```kotlin
   springBoot {
       mainClass.set("seminars.Main")
   }
   ```

3. **Конфликт `application.yaml` / `application.yml`** — два файла настроек
   от разных сервисов оказались в одной `src/main/resources`, второй
   (`web-application-type: none`) отключал веб-сервер. Переименован в
   `application.yml.disabled`.

4. **`No property 'isActive' found for type 'Satellite'`** — методы
   `SatelliteRepository.findByIsActiveTrue()` (и его использование в
   `SatelliteController`/`SpaceOperationController`) ссылались на
   несуществующее плоское поле; реальное поле лежит во вложенном `state`.
   Переименовано в `findByStateActiveTrue()`.

5. **`batteryLevel (0.85) должен быть в диапазоне [0.0, 0.0]`** — конструктор
   `Satellite(String name, double batteryLevel)` вызывал
   `EnergySystem.builder()` без `.maxBattery()`/`.minBattery()`, из-за чего
   у примитивов `double` подставлялись нули по умолчанию. Добавлены
   значения по умолчанию из констант `EnergySystem`.

6. **`bitnami/kafka:3.7.0: not found`** — с 28 августа 2025 Broadcom убрал
   версионные образы Bitnami из бесплатного публичного Docker Hub
   (см. [обсуждение в issue tracker Bitnami](https://github.com/bitnami/charts/issues/35164)).
   В `docker-compose.yml` образ Kafka заменён на официальный `apache/kafka:3.7.0`,
   с соответствующей адаптацией имён переменных окружения (без префикса
   `KAFKA_CFG_`, только `KAFKA_`) и путей volume/healthcheck.

Если ваша копия репозитория уже содержит эти фиксы — раздел можно
игнорировать.
</details>

### 4. Запуск нагрузочного теста

**Linux / macOS / Git Bash / WSL:**

```bash
chmod +x load-tests/run.sh   # один раз, если права на исполнение не сохранились при клонировании
./load-tests/run.sh
```

**Windows PowerShell** (нативный вариант, без Git Bash/WSL):

```powershell
.\load-tests\run.ps1
```

Если PowerShell блокирует запуск скрипта («выполнение сценариев отключено»),
разрешите его один раз для текущей сессии терминала (это не меняет
системную политику навсегда):

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

Оба скрипта делают одно и то же:
1. Проверяют, что `http://localhost:8080` отвечает
2. Запускают k6 (разгон → 60с удержания пика → спад), пишут сырые результаты
   в `load-tests/results/raw-results.json` и нативный HTML-отчёт в
   `load-tests/results/summary.html`
3. Конвертируют результаты в `load-tests/allure-results/`
4. Генерируют Allure-отчёт в `load-tests/allure-report/`

Настройка пикового числа пользователей и адреса сервиса:

```bash
# bash
VUS_MAX=50 BASE_URL=http://localhost:8080 ./load-tests/run.sh
```
```powershell
# PowerShell
$env:VUS_MAX=50; $env:BASE_URL="http://localhost:8080"; .\load-tests\run.ps1
```

### 5. Просмотр отчёта

**Allure:**
```bash
allure open load-tests/allure-report
# либо, если allure не установлен глобально:
npx allure-commandline open load-tests/allure-report
```

**Нативный HTML-отчёт k6** (быстрый способ без дополнительных шагов):
```bash
open load-tests/results/summary.html   # macOS
xdg-open load-tests/results/summary.html   # Linux
start load-tests/results/summary.html   # Windows
```

---

## Как читать Allure-отчёт

Каждая завершённая итерация сценария (один виртуальный пользователь,
один законченный проход всех 7 шагов) отображается как отдельный тесткейс
с именем `Сценарий оператора ЦУП — итерация VU{n}-Iter{m}-{timestamp}`.
Внутри тесткейса — 7 шагов в порядке выполнения, каждый с HTTP-статусом и
длительностью запроса. Тесткейс помечен `failed`, если хотя бы один шаг
получил не тот HTTP-код, который ожидался (см. таблицу
`EXPECTED_STATUS` в [`load-tests/allure-adapter/convert.js`](./load-tests/allure-adapter/convert.js));
конкретный упавший шаг и ожидаемый/полученный код видны в `statusDetails`
тесткейса и в статусе самого шага.

---

## Проект

Основной код сервиса — в `src/main/java/seminars` (Spring Boot, REST API
управления спутниковыми группировками, порт 8080). Смежные модули —
`telemetry` (gRPC-сервис телеметрии, порт 9091) и `scheduler` (плановый
запуск миссий по расписанию, без собственного REST API) — не участвуют
в данном нагрузочном тесте: он покрывает только публичный REST API
основного сервиса, который используется реальными клиентами.
