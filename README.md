# 🛰️ Satellite Management System — Семинар 8 (REST API + Mission Scheduler)

Основной сервис обзавёлся HTTP API (Spring MVC + Swagger), а рядом поднялся отдельный микросервис-планировщик, который по расписанию дёргает этот API через `RestClient`.

---

## 📦 Два архива

| Архив | Что внутри |
|---|---|
| `satellite-spring.zip` | Основной сервис с REST-контроллером, порт **8080** |
| `satellite-scheduler.zip` | Микросервис-планировщик, порт **8081** |

---

## ЧАСТЬ A: REST API основного сервиса (`satellite-spring`)

### Что добавилось

```
build.gradle.kts          ← spring-boot-starter-web + springdoc-openapi
application.yaml          ← заменил application.properties, port: 8080
seminars/factory/
  SatelliteParam.java     ← @JsonTypeInfo + @JsonSubTypes для полиморфной десериализации
  CommunicationSatelliteParam.java  ← @JsonCreator
  ImagingSatelliteParam.java        ← @JsonCreator
seminars/facade/
  AddSatelliteRequest.java     ← @JsonCreator
  MissionRequest.java          ← @JsonCreator
seminars/controller/
  SpaceOperationController.java    ← @RestController, 6 эндпоинтов
  GlobalExceptionHandler.java      ← @RestControllerAdvice, JSON-ответы при ошибках
src/test/java/seminars/controller/
  SpaceOperationControllerTest.java  ← @SpringBootTest + @AutoConfigureMockMvc, 8 тестов
```

### Эндпоинты

| Метод | URL | Описание |
|---|---|---|
| `POST` | `/api/add-satellites` | Добавить спутники в группировку (создаёт её, если не было) |
| `POST` | `/api/missions` | Выполнить миссию группировки |
| `POST` | `/api/deploy` | Полный цикл: создание + активация + миссии |
| `GET` | `/api/overview` | Сводка по всем группировкам |
| `GET` | `/api/constellations/{name}/report` | Отчёт по конкретной группировке |
| `DELETE` | `/api/constellations/{name}/satellites/{sat}` | Вывести спутник из эксплуатации |

### Swagger UI
После запуска: **http://localhost:8080/swagger-ui.html**

### Пример запроса (POST /api/add-satellites)

```json
{
  "constellationName": "Орбита-1",
  "satelliteParams": [
    {
      "type": "COMMUNICATION",
      "name": "Связь-1",
      "batteryLevel": 0.85,
      "bandwidth": 500.0
    },
    {
      "type": "IMAGE",
      "name": "ДЗЗ-1",
      "batteryLevel": 0.92,
      "resolution": 2.5
    }
  ]
}
```

> Поле `"type"` обязательно — по нему Jackson понимает, в `CommunicationSatelliteParam` или `ImagingSatelliteParam` превращать объект (`@JsonTypeInfo` + `@JsonSubTypes` на `SatelliteParam`).

### Запуск
```bash
cd satellite-spring
./gradlew bootRun
```

---

## ЧАСТЬ B: Mission Scheduler (`satellite-scheduler`)

### Структура

```
scheduler/
  SchedulerApplication.java           # @EnableScheduling + @EnableConfigurationProperties
  properties/SpaceCenterProperties.java  # @ConfigurationProperties record
  configuration/RestClientConfiguration.java  # бин RestClient с baseUrl из properties
  client/SpaceOperationClient.java     # HTTP-клиент (RestClient) для вызова основного сервиса
  missions/
    MissionRequest.java                # record-DTO для POST /api/missions
    ConfiguredMissionScheduler.java    # @PostConstruct → schedule(Runnable, CronTrigger)
```

### Конфигурация (application.yml)

```yaml
server:
  port: 8081

app:
  space-center-service:
    url: "http://localhost:8080/api"
    missions:
      - targetType: CONSTELLATION
        constellationName: "GeoStationary"
        cron: "0 0 */6 * * *"    # каждые 6 часов

      - targetType: SINGLE_SATELLITE
        constellationName: "LowOrbit"
        satelliteName: "Sat-1"
        cron: "0 30 8 * * MON"   # каждый понедельник в 8:30

      - targetType: CONSTELLATION
        constellationName: "TestConstellation"
        cron: "0 */1 * * * *"    # каждую минуту
```

### Как работает

1. `@EnableConfigurationProperties(SpaceCenterProperties.class)` → Spring читает YAML → заполняет record
2. `RestClientConfiguration` создаёт бин `RestClient` с `baseUrl` из properties
3. `ConfiguredMissionScheduler.scheduleMissions()` (через `@PostConstruct`) проходит по списку миссий и вызывает `TaskScheduler.schedule(task, new CronTrigger(cron))` для каждой
4. В заданное время планировщик вызывает `SpaceOperationClient.executeMission(request)`, который делает `POST /api/missions` на основной сервис
5. Ошибки (сервис недоступен, таймаут, HTTP 5xx) логируются через SLF4J и **не роняют** весь планировщик — `try-catch Exception` в задаче

### Валидация конфигурации
- `SINGLE_SATELLITE` без `satelliteName` → `IllegalStateException` при старте
- Миссия без `constellationName` → `IllegalStateException`
- Миссия без `cron` → `IllegalStateException`

### Запуск (основной сервис должен работать)
```bash
# Терминал 1
cd satellite-spring
./gradlew bootRun

# Терминал 2
cd satellite-scheduler
./gradlew bootRun
```

---

## 🧪 Тесты

**Основной сервис:**
- `SpaceOperationControllerTest` — 8 тестов через `MockMvc` (`@SpringBootTest` + `@AutoConfigureMockMvc`): все 6 эндпоинтов, включая граничные случаи 404 и 422

**Планировщик:**
- `ConfiguredMissionSchedulerTest` — юнит-тест с мок-объектами: регистрация 2 задач, пустой список, SINGLE_SATELLITE без имени спутника → исключение, SINGLE_SATELLITE с именем → OK

```bash
./gradlew test jacocoTestReport   # для основного сервиса
./gradlew test                    # для планировщика
```

---

## ⚠️ Важное про полиморфную десериализацию

Без `@JsonTypeInfo`/`@JsonSubTypes` Spring не знает, в какой конкретный класс превращать абстрактный `SatelliteParam` из JSON-тела запроса. Поле `"type"` в JSON — это `SatelliteType` enum, его значение (`COMMUNICATION` или `IMAGE`) используется как дискриминатор. `@JsonCreator` на конструкторах подклассов нужен, потому что у них нет дефолтного конструктора (поля `final`), а Jackson по умолчанию ищет именно его.

---

## 📚 Технологии

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?logo=springboot)
![Spring Web](https://img.shields.io/badge/Spring%20Web-MVC-blue)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI%203-green)
![RestClient](https://img.shields.io/badge/RestClient-3.2%2B-lightblue)
![Lombok](https://img.shields.io/badge/Lombok-enabled-red)
