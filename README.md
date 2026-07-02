# 🛰️ Satellite Management System — Семинар 9.2 (JPA + PostgreSQL)

Добавлена поддержка PostgreSQL через Spring Data JPA и Flyway. Данные сохраняются между перезапусками.

---

## 📋 Что изменилось

| Было | Стало |
|---|---|
| `ConstellationRepository` — `HashMap<String, SatelliteConstellation>` | **Удалён**, заменён на `SatelliteConstellationRepository extends JpaRepository` |
| Данные в памяти — теряются при рестарте | Данные в PostgreSQL — переживают рестарты |
| Доменные классы — чистые Java-объекты | Доменные классы — JPA-сущности с аннотациями |

---

## 🗂️ Изменённые / добавленные файлы

```
build.gradle.kts           ← + jpa, postgresql, flyway, h2 (test)
application.yaml           ← + datasource, jpa, flyway config (env-vars)
src/main/resources/db/migration/
  V1__init_schema.sql      ← НОВЫЙ: Flyway-миграция, 3NF-схема, индексы

domain/
  EnergySystem.java        ← @Embeddable + protected no-arg ctor для JPA
  SatelliteState.java      ← @Embeddable + protected no-arg ctor
  Satellite.java           ← @Entity @Inheritance(JOINED) @DiscriminatorColumn
  CommunicationSatellite.java ← @Entity @Table @DiscriminatorValue("COMMUNICATION")
  ImagingSatellite.java    ← @Entity @Table @DiscriminatorValue("IMAGE")
  SatelliteConstellation.java ← @Entity @OneToMany(cascade=ALL) @JsonManagedReference

repository/
  ConstellationRepository.java ← УДАЛЁН (HashMap)
  SatelliteConstellationRepository.java ← НОВЫЙ: JpaRepository + findByConstellationNameWithSatellites
  SatelliteRepository.java ← НОВЫЙ: JpaRepository + findByIsActiveTrue

service/ConstellationService.java  ← @Transactional, использует JPA-репозитории
controller/
  ConstellationController.java ← НОВЫЙ: CRUD для группировок
  SatelliteController.java     ← НОВЫЙ: CRUD для спутников
Main.java  ← использует SatelliteConstellationRepository

test/resources/application-test.yaml ← НОВЫЙ: H2 + flyway=false для тестов
test/repository/
  SatelliteConstellationRepositoryTest.java ← НОВЫЙ: @DataJpaTest + H2
  SatelliteRepositoryTest.java              ← НОВЫЙ: @DataJpaTest + H2
  ConstellationRepositoryMockTest.java      ← переписан под JPA-моки
  ConstellationRepositoryIntegrationTest.java ← переписан с @ActiveProfiles("test")
```

---

## 🏗️ Схема БД (3NF, Flyway V1)

```
satellite_constellations (id PK, constellation_name UNIQUE)
         ↑ FK (constellation_id)
satellites (id PK, name, satellite_type, is_active, status_message,
            battery_level, max_battery, min_battery, low_battery_threshold)
         ↑ FK (id)                    ↑ FK (id)
communication_satellites(bandwidth)   imaging_satellites(resolution, photos_taken)

Индексы:
  idx_constellation_name   — уникальный, поиск по имени (частый, редко меняется)
  idx_satellite_is_active  — фильтр активных спутников
  idx_satellite_constellation — JOIN satellites → constellation
```

---

## ❓ Ответ: @Embedded vs @OneToOne

**`@Embedded` (`EnergySystem`, `SatelliteState`)**:
- Не существуют без спутника → нет смысла хранить отдельно
- Нет потребности в прямом API-доступе (нет `/api/energy-systems`)
- Поля функционально зависят только от PK спутника → 3NF не нарушается при включении в ту же таблицу
- Меньше JOIN-ов при чтении → лучше производительность

**`@OneToOne` было бы уместно, если бы**:
- Объект имел отдельный жизненный цикл (можно создать без спутника)
- Требовался прямой репозиторий / API-доступ
- Таблица `satellites` стала слишком широкой из-за embedded-полей
- Объект разделялся между несколькими спутниками (`@ManyToOne`)

---

## ⚡ Ключевые технические детали

### JPA Inheritance: JOINED
```java
@Entity
@Table(name = "satellites")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "satellite_type")
public abstract class Satellite { ... }

@Entity @Table(name = "communication_satellites")
@DiscriminatorValue("COMMUNICATION")
public class CommunicationSatellite extends Satellite { ... }
```

### Bidirectional @OneToMany — управление обеими сторонами
```java
// В SatelliteConstellation.addSatellite():
satellites.add(satellite);
satellite.setConstellation(this);  // ОБЯЗАТЕЛЬНО для FK constellation_id
```
Без `setConstellation(this)` Hibernate не запишет FK и спутник останется без группировки.

### @Transactional + dirty checking
```java
@Transactional
public void activateAllSatellites(String name) {
    SatelliteConstellation c = getConstellationOrThrow(name);
    for (Satellite s : c.getSatellites()) s.activate();
    // Hibernate автоматически сохранит is_active при закрытии транзакции
}
```

### N+1 проблема и JOIN FETCH
```java
@Query("SELECT c FROM SatelliteConstellation c LEFT JOIN FETCH c.satellites WHERE c.constellationName = :name")
Optional<SatelliteConstellation> findByConstellationNameWithSatellites(String name);
```
Без `JOIN FETCH` доступ к `constellation.getSatellites()` с `FetchType.LAZY` вне транзакции → `LazyInitializationException`.

---

## 🧪 Стратегия тестирования

| Тест | Аннотация | БД |
|---|---|---|
| `SatelliteConstellationRepositoryTest` | `@DataJpaTest` | H2 auto |
| `SatelliteRepositoryTest` | `@DataJpaTest` | H2 auto |
| `ConstellationRepositoryMockTest` | `@ExtendWith(Mockito)` | нет |
| `ConstellationRepositoryIntegrationTest` | `@SpringBootTest @ActiveProfiles("test")` | H2 |
| Все остальные `@SpringBootTest` | `@ActiveProfiles("test")` | H2 |

Для production-близких тестов — используй **Testcontainers** с реальным PostgreSQL.

---

## 🚀 Запуск

### Локально (PostgreSQL через Docker)
```bash
# Только база данных:
docker compose up postgres -d

# Приложение (Flyway создаст схему при первом запуске):
cd satellite-spring && ./gradlew bootRun
```

### Полный стек в Docker
```bash
docker compose up --build
```

### Тесты (H2, без PostgreSQL)
```bash
cd satellite-spring && ./gradlew test jacocoTestReport
```

---

## 📚 Технологии

![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-enabled-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Flyway](https://img.shields.io/badge/Flyway-migrations-red)
![H2](https://img.shields.io/badge/H2-test--only-orange)
