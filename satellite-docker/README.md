# 🛰️ Satellite System — Docker Deployment

Два Spring Boot сервиса в Docker, оркестрированных через Docker Compose.

## 📁 Структура репозитория

```
/
├── satellite-spring/          # Основной сервис (порт 8080)
│   ├── src/
│   ├── build.gradle.kts
│   ├── Dockerfile             ← многостадийная сборка
│   └── .dockerignore
│
├── satellite-scheduler/       # Планировщик миссий (порт 8081)
│   ├── src/
│   ├── build.gradle.kts
│   ├── Dockerfile
│   └── .dockerignore
│
├── docker-compose.yml         ← оркестрация обоих сервисов
└── .github/workflows/
    └── ci-cd.yml              ← GitHub Actions (тесты → SAST → Docker push → DAST)
```

> ⚠️ `docker-compose.yml` должен лежать в корне репозитория рядом с папками `satellite-spring/` и `satellite-scheduler/`.

---

## 🚀 Быстрый старт

```bash
# Собрать образы и запустить оба контейнера
docker compose up --build

# В фоне:
docker compose up --build -d

# Остановить и удалить контейнеры:
docker compose down
```

После старта:

| Сервис | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Healthcheck | http://localhost:8080/actuator/health |
| Планировщик | http://localhost:8081 |

---

## 🔍 Как проверить, что всё работает

```bash
# Статус контейнеров (server должен быть healthy)
docker compose ps

# Логи обоих сервисов в реальном времени
docker compose logs -f

# Логи только планировщика
docker compose logs -f mission-service

# Отправить тестовый запрос через curl
curl -X GET http://localhost:8080/api/overview

# Или через Postman / Swagger UI
```

---

## 🌐 Переменные окружения

| Переменная | Сервис | Описание | Значение по умолчанию |
|---|---|---|---|
| `SERVER_PORT` | оба | Порт, на котором слушает сервис | `8080` / `8081` |
| `SERVER_URL` | scheduler | Базовый URL основного сервиса | `http://localhost:8080` |

В Docker-сети сервисы находятся в одной сети `satellite-net` и доступны по имени сервиса (`server`), а не по `localhost`. Именно поэтому планировщик получает `SERVER_URL=http://server:8080`.

---

## 🏗️ Многостадийная сборка

```
Stage 1 (build): gradle:8.12-jdk21
  └── ./gradlew bootJar -x test
      └── build/libs/*.jar

Stage 2 (run): eclipse-temurin:21-jre-alpine
  └── COPY app.jar
      └── java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar app.jar
```

Финальный образ содержит только JRE (без JDK, Gradle, исходников) → меньший размер и меньшая поверхность атаки.

---

## 🔒 Безопасность

- Контейнеры запускаются от непривилегированного пользователя `appuser` (не `root`)
- Actuator открывает только `/actuator/health` (остальные эндпоинты закрыты)
- CI/CD включает: CodeQL (SAST) + Trivy (DAST — сканирование образов)

---

## ⚙️ CI/CD (GitHub Actions)

Пайплайн (`.github/workflows/ci-cd.yml`) при пуше в `main`:

1. Тесты `satellite-spring` (`./gradlew test`)
2. Тесты `satellite-scheduler` (`./gradlew test`)
3. SAST — CodeQL анализ Java-кода
4. Docker build + push в GitHub Container Registry (GHCR)
   - Теги: `latest` + короткий SHA коммита
5. DAST — Trivy сканирование опубликованных образов

Образы хранятся в GHCR (`ghcr.io/<ваш-username>/satellite-spring:latest`).
`GITHUB_TOKEN` создаётся автоматически — дополнительных секретов не нужно.
