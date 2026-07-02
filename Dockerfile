# ─────────────────────────────────────────────
# Stage 1: Build
# Используем официальный образ с Gradle и JDK 21.
# Это «тяжёлый» образ (~700 MB), но он нужен только для сборки —
# в финальный образ он не попадёт.
# ─────────────────────────────────────────────
FROM gradle:8.12-jdk21 AS build

WORKDIR /app

# Копируем описание сборки первыми — Docker кэширует слои.
# Если изменились только .java-файлы, зависимости не перекачиваются.
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./

# Загружаем зависимости отдельным слоем (кэш)
RUN ./gradlew dependencies --no-daemon -q

# Копируем исходники и собираем JAR (без тестов — они запускаются в CI)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ─────────────────────────────────────────────
# Stage 2: Run
# Минимальный образ: только JRE без компилятора, Maven-репо и прочего мусора.
# eclipse-temurin — официальная реализация OpenJDK с регулярными патчами.
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS run

WORKDIR /app

# Создаём непривилегированного пользователя — запускать JVM от root
# опасно: потенциальная уязвимость в JVM или приложении даёт атакующему
# root внутри контейнера, что облегчает побег в хост.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Копируем только готовый JAR из build-стадии
COPY --from=build /app/build/libs/*.jar app.jar

# Убираем права на запись у appuser — JAR читается, но не перезаписывается
RUN chown appuser:appgroup app.jar

USER appuser

# Явно документируем порт (не обязательно, но полезно для читаемости)
EXPOSE 8080

# JVM-флаги:
#  -XX:+UseContainerSupport  — JVM учитывает лимиты памяти контейнера, а не RAM хоста
#  -XX:MaxRAMPercentage=75.0 — отдаём JVM 75% выделенной контейнеру памяти
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
