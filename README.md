# 🛰️ Satellite Management System — Семинар 11 (gRPC Server Streaming)

Добавлен третий микросервис `satellite-telemetry`, генерирующий телеметрию спутников через gRPC Server Streaming. Основной сервис подписывается на поток и сохраняет данные температуры в PostgreSQL.

---

## 📦 Три архива

| Архив | Что внутри |
|---|---|
| `satellite-spring.zip` | Основной сервис (порт 8080, REST + gRPC клиент) |
| `satellite-telemetry.zip` | Новый gRPC-сервис телеметрии (порт 9091) |
| `satellite-docker.zip` | docker-compose + Dockerfiles (все 4 сервиса) |

---

## ⚡ Обязательный шаг перед сборкой

Оба проекта (`satellite-spring` и `satellite-telemetry`) содержат `.proto`-файл. Java-классы из него генерируются Gradle-плагином `com.google.protobuf`:

```bash
# В satellite-telemetry:
cd satellite-telemetry && ./gradlew generateProto

# В satellite-spring:
cd satellite-spring && ./gradlew generateProto
```

Без этого шага проекты не скомпилируются — `TelemetryServiceGrpc`, `TelemetryRequest`, `TelemetryUpdate` не существуют до генерации.

В Docker-сборке (`Dockerfile`) `bootJar` запускает `generateProto` автоматически (Gradle граф задач).

---

## 🗂️ Новые файлы

```
satellite-telemetry/                    ← НОВЫЙ микросервис
├── src/main/proto/telemetry.proto      ← gRPC контракт
├── src/main/java/telemetry/
│   ├── TelemetryApplication.java
│   └── service/TelemetryGrpcService.java  ← @GrpcService, Server Streaming
└── src/main/resources/application.yml     ← grpc.server.port: 9091

satellite-spring/                       ← обновлён
├── src/main/proto/telemetry.proto      ← COPY для генерации клиентского кода
├── src/main/java/seminars/
│   ├── telemetry/
│   │   ├── TelemetryGrpcClient.java    ← @GrpcClient, подписка на поток
│   │   └── TelemetryUpdateService.java ← @Transactional, сохранение в DB
│   ├── controller/TelemetryController.java ← REST /api/telemetry
│   └── domain/Satellite.java           ← + internalTemperature, externalTemperature
├── src/main/resources/db/migration/
│   └── V2__add_temperature_columns.sql ← Flyway: 2 nullable колонки
└── build.gradle.kts                    ← + grpc-client-starter + protobuf plugin
```

---

## 📡 gRPC контракт (telemetry.proto)

```proto
service TelemetryService {
  rpc StreamTelemetry(TelemetryRequest) returns (stream TelemetryUpdate);
}
```

**Server Streaming** — клиент делает один запрос, сервер непрерывно шлёт обновления:

```
satellite-spring                   satellite-telemetry
  │                                         │
  │──── StreamTelemetry(request) ────────▶  │
  │                                         │
  │◀─── TelemetryUpdate (Связь-1) ────────  │  каждые 2 секунды
  │◀─── TelemetryUpdate (ДЗЗ-1) ──────────  │  для каждого спутника
  │◀─── TelemetryUpdate (ДЗЗ-2) ──────────  │
  │           ...                            │
```

---

## 🌡 Эмулируемые данные

| Поле | Диапазон | Смысл |
|---|---|---|
| `internalTemperature` | 15–35°C | Температура электроники (норма) |
| `externalTemperature` | -150 до +120°C | Корпус: тень (-150°C) → Солнце (+120°C) |
| `batteryLevel` | 0.3–1.0 | Заряд батареи |

---

## 🔄 Поток данных

```
[telemetry-service] generateRandomTelemetry() → onNext(TelemetryUpdate)
         ↓ gRPC stream (port 9091)
[satellite-spring] TelemetryGrpcClient.onNext() → TelemetryUpdateService.applyTelemetryUpdate()
         ↓ @Transactional
[PostgreSQL] UPDATE satellites SET internal_temperature=?, external_temperature=? WHERE name=?
         ↓ REST API
[Client] GET /api/telemetry → [{"name":"Связь-1","internalTemperature":22.5,...}]
```

---

## ⚙️ Ключевые детали реализации

### @ConditionalOnProperty на клиенте
```java
@ConditionalOnProperty(name = "telemetry.client.enabled", havingValue = "true")
public class TelemetryGrpcClient { ... }
```
В `application-test.yaml`: `telemetry.client.enabled: false` — не пытается подключиться в тестах.

### Retry при ошибке подключения
```java
@Override
public void onError(Throwable t) {
    retryExecutor.schedule(this::connect, 30, TimeUnit.SECONDS);
}
```
Если `telemetry-service` недоступен — пробует снова каждые 30 секунд.

### @Transactional в отдельном сервисе
`onNext()` callback работает в gRPC-потоке (не Spring-managed). Вызов `@Transactional`-метода через Spring-прокси возможен только из другого `@Service`. Поэтому обновление БД вынесено в `TelemetryUpdateService`.

### Отмена стрима при дисконнекте клиента
```java
serverObserver.setOnCancelHandler(() -> {
    executor.shutdown();  // останавливаем ScheduledExecutorService
});
```

---

## 🚀 Запуск

### Все 4 сервиса в Docker
```bash
docker compose up --build
```

### Локально (последовательно):
```bash
# 1. Телеметрия
cd satellite-telemetry && ./gradlew generateProto bootRun

# 2. Основной сервис (PostgreSQL должен быть запущен)
cd satellite-spring && ./gradlew generateProto bootRun

# 3. Планировщик
cd satellite-scheduler && ./gradlew bootRun
```

### Проверка телеметрии через REST
```bash
# После появления данных (~5 сек):
curl http://localhost:8080/api/telemetry
```

### Проверка gRPC-стрима напрямую (grpcurl)
```bash
grpcurl -plaintext -d '{"satellite_names":["Связь-1","ДЗЗ-1"]}' \
  localhost:9091 telemetry.TelemetryService/StreamTelemetry
```

---

## 🧪 Тесты

| Файл | Что проверяет |
|---|---|
| `TelemetryUpdateServiceTest` | Mockito-тест: обновляет температуру / пропускает неизвестные спутники |
| `TelemetryControllerTest` | MockMvc: GET /api/telemetry, 404 для несуществующего ID |

---

## 📐 Docker Compose (4 сервиса)

```
postgres:9432       ← PostgreSQL с именованным томом
telemetry-service:9091  ← gRPC Server Streaming
server:8080         ← REST + JPA + gRPC Client (зависит от postgres + telemetry-service)
mission-service:8081    ← REST планировщик (зависит от server)
```

---

## 📚 Технологии

![gRPC](https://img.shields.io/badge/gRPC-Server%20Streaming-blue)
![Protobuf](https://img.shields.io/badge/Protobuf-3.25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Flyway](https://img.shields.io/badge/Flyway-V2-red)
