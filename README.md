# 🛰️ Satellite Management System — Семинары 3–4 (Spring Boot, DI, Lombok, тесты)

Система управления спутниковой группировкой на **Spring Boot**, с реализацией принципа **Dependency Inversion**, кодогенерацией через **Lombok** и тремя видами тестов (Unit / Mock / Integration) с отчётом покрытия **JaCoCo**.

---

## 📋 Что добавилось по сравнению с семинаром 2

| Семинар | Что добавлено |
|---|---|
| 3 | Spring Boot, `ConstellationRepository`, `SpaceOperationCenterService`, DI через конструктор |
| 4 | Lombok (переписан весь продакшен-код), Unit/Mock/Integration-тесты, JaCoCo |

---

## 🗂️ Структура проекта

```
satellite-spring/
├── build.gradle.kts                 # Spring Boot + Lombok + JaCoCo
├── settings.gradle.kts
├── gradlew / gradlew.bat            # запуск без локального Gradle
├── src/main/resources/application.properties
└── src/main/java/seminars/
    ├── Main.java                            # @SpringBootApplication
    ├── domain/                              # обычные классы, Spring их не создаёт
    │   ├── Satellite.java
    │   ├── EnergySystem.java
    │   ├── SatelliteState.java
    │   ├── CommunicationSatellite.java
    │   ├── ImagingSatellite.java
    │   └── SatelliteConstellation.java
    ├── repository/
    │   └── ConstellationRepository.java     # @Repository, хранилище в Map
    └── service/
        └── SpaceOperationCenterService.java # @Service, DI через конструктор
└── src/test/java/seminars/repository/
    ├── ConstellationRepositoryUnitTest.java
    ├── ConstellationRepositoryMockTest.java
    └── ConstellationRepositoryIntegrationTest.java
```

---

## 🔄 Dependency Inversion Principle

- `SpaceOperationCenterService` не создаёт `ConstellationRepository` сам (`new ...`) — получает готовый экземпляр через конструктор
- `Main` не создаёт ни репозиторий, ни сервис вручную — забирает их из `ConfigurableApplicationContext` через `getBean(...)`
- Единственное место, которое «знает», как всё собрать — это сам Spring-контейнер; остальной код зависит только от абстракций

---

## 🧬 Где использован Lombok (и где сознательно нет)

| Класс | Аннотации | Почему |
|---|---|---|
| `EnergySystem` | `@Getter @ToString @AllArgsConstructor` | один мутируемый филд — идеальный кандидат |
| `SatelliteState` | `@Getter @ToString @NoArgsConstructor` | дефолты через инициализаторы полей |
| `Satellite` | `@Getter` только на `name`, `@ToString` | конструктор с побочным эффектом (println) — вручную; `state`/`energy` не выставлены наружу (инкапсуляция) |
| `ImagingSatellite` / `CommunicationSatellite` | `@Getter @ToString(callSuper=true)` | конструктор зовёт `super(...)` — Lombok так не умеет |
| `SatelliteConstellation` | `@Getter @ToString` | конструктор с println — вручную |
| `ConstellationRepository` | **без Lombok** | там нет геттер-бойлерплейта, только CRUD-логика |
| `SpaceOperationCenterService` | `@RequiredArgsConstructor` | классика для Spring-сервиса с одной DI-зависимостью |

⚠️ `toString()` теперь в формате Lombok `ClassName(field=value)` (круглые скобки, без кавычек у строк) вместо прежнего `ClassName{field=value}`.

---

## 🧪 Тесты

Все три класса лежат в `seminars.repository`, чтобы зеркалить пакет `ConstellationRepository`.

- **`ConstellationRepositoryUnitTest`** — без Spring, `new ConstellationRepository()`, проверяет все CRUD-методы (позитивные/негативные сценарии + граничный случай с пустой строкой)
- **`ConstellationRepositoryMockTest`** — `@Mock ConstellationRepository` + `@InjectMocks SpaceOperationCenterService`. Поскольку у самого репозитория нет зависимостей для мока, тест мокает репозиторий и проверяет, что сервис правильно с ним взаимодействует (`when().thenReturn()`, `verify()`)
- **`ConstellationRepositoryIntegrationTest`** — `@SpringBootTest`, `@Autowired` на оба бина, полный жизненный цикл объекта: создание группировки → добавление спутников → активация → выполнение миссий, с проверками между шагами

---

## 🚀 Запуск

Требуется Java 21+ и доступ в интернет (для первой загрузки зависимостей Gradle).

```bash
# Запуск приложения
./gradlew bootRun

# Запуск тестов
./gradlew test

# Тесты + отчёт о покрытии
./gradlew test jacocoTestReport
```

Отчёты после запуска:
- Результаты тестов — `build/reports/tests/test/index.html`
- Покрытие JaCoCo — `build/reports/jacoco/test/html/index.html`

---

## ⚠️ Известное ограничение проверки

Сборка и тесты сгенерированы и логически проверены (продакшен-логика дополнительно сверена вручную через «развёрнутый» эквивалент Lombok-кода), но **не прогнаны вживую** — у среды, в которой создавался проект, нет доступа к Maven Central. Перед сдачей обязательно прогони `./gradlew test` локально.

---

## 📚 Технологии

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?logo=springboot)
![Lombok](https://img.shields.io/badge/Lombok-enabled-red)
![JaCoCo](https://img.shields.io/badge/JaCoCo-coverage-yellow)
