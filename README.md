# 🛰️ Satellite Management System — Семинар 5 (Factory Method + Builder)

Рефакторинг создания объектов с использованием порождающих паттернов GoF: **Factory Method** для спутников и **Builder** для `EnergySystem`.

---

## 📋 Что изменилось по сравнению с семинарами 3–4

| Было | Стало |
|---|---|
| `new CommunicationSatellite(...)` / `new ImagingSatellite(...)` разбросаны по `Main` и тестам | Создание идёт через `SatelliteFactory` → `CommunicationSatelliteFactory` / `ImagingSatelliteFactory` |
| `new EnergySystem(batteryLevel)` | Публичного конструктора нет вообще — только `EnergySystem.builder()...build()` |
| `EnergySystem` хранил только `batteryLevel`, `consume()` был `void` | Добавлены `maxBattery`, `minBattery`, `lowBatteryThreshold`; `consume()`/`recharge()` возвращают `boolean` |
| `hasEnoughCharge()` | Переименован в `hasSufficientPower()` |

---

## 🗂️ Структура проекта

```
satellite-spring/
└── src/main/java/seminars/
    ├── Main.java                              # достаёт фабрики из контекста, создаёт спутники через них
    ├── domain/
    │   ├── EnergySystem.java                  # ПЕРЕРАБОТАН: только через Builder
    │   ├── Satellite.java                     # строит EnergySystem через builder()
    │   ├── SatelliteState.java                # без изменений
    │   ├── CommunicationSatellite.java        # использует boolean-результат consume()
    │   ├── ImagingSatellite.java
    │   └── SatelliteConstellation.java
    ├── factory/                               # НОВЫЙ пакет
    │   ├── SatelliteFactory.java              # абстрактная фабрика (Factory Method)
    │   ├── CommunicationSatelliteFactory.java # @Component
    │   └── ImagingSatelliteFactory.java       # @Component
    ├── repository/ConstellationRepository.java
    └── service/SpaceOperationCenterService.java
└── src/test/java/seminars/
    ├── domain/EnergySystemTest.java           # НОВЫЙ: тесты Builder'а и бизнес-логики
    ├── factory/SatelliteFactoryTest.java      # НОВЫЙ: тесты обеих фабрик
    └── repository/
        ├── ConstellationRepositoryUnitTest.java        # без изменений
        ├── ConstellationRepositoryMockTest.java         # спутники теперь через фабрику
        └── ConstellationRepositoryIntegrationTest.java  # фабрики теперь @Autowired
```

---

## 🏭 Factory Method

```java
public abstract class SatelliteFactory {
    public abstract Satellite createSatellite(String name, double batteryLevel);
    public abstract Satellite createSatelliteWithParameter(String name, double batteryLevel, double parameter);
}
```

- `createSatellite(name, batteryLevel)` — создаёт спутник со значением "характерного" параметра по умолчанию (`DEFAULT_BANDWIDTH = 500.0` для связи, `DEFAULT_RESOLUTION = 1.0` для ДЗЗ)
- `createSatelliteWithParameter(...)` — то же самое, но с явно заданным параметром
- Обе конкретные фабрики — Spring-бины (`@Component`), получаются в `Main` через `context.getBean(...)`, как и `ConstellationRepository`/`SpaceOperationCenterService`

---

## 🔧 Builder для EnergySystem

```java
EnergySystem energy = EnergySystem.builder()
        .batteryLevel(0.85)
        .lowBatteryThreshold(0.25)   // необязательно, дефолт 0.2
        .maxBattery(1.0)             // необязательно, дефолт 1.0
        .minBattery(0.0)             // необязательно, дефолт 0.0
        .build();
```

- Конструктор **приватный** — снаружи объект собрать иначе, чем через билдер, нельзя
- Реализовано через Lombok `@Builder` на приватном конструкторе + `@Builder.Default` на полях — это даёт одновременно и кодогенерацию, и место для валидации
- Валидация (границы `min ≤ batteryLevel ≤ max`, `min ≤ threshold ≤ max`, `min ≤ max`) живёт **в конструкторе**, а не размазана по `consume()`/`recharge()` — методы всегда работают с уже корректным состоянием
- `consume(amount)` / `recharge(amount)` зажимают результат по границам и возвращают `boolean` — `false`, если запрошенное значение вышло за границу

---

## 🧪 Тесты

- **`EnergySystemTest`** — варианты сборки билдером (дефолты / частичная кастомизация / полная кастомизация), все три сценария валидации (`IllegalArgumentException`), границы `consume()`/`recharge()`, граница `hasSufficientPower()`/`isCritical()`
- **`SatelliteFactoryTest`** — обе фабрики, оба метода (`createSatellite`/`createSatelliteWithParameter`), проверка конкретного типа через `assertInstanceOf`
- Существующие Mock/Integration-тесты обновлены: спутники в них создаются через фабрики (мок-тест — `new CommunicationSatelliteFactory()`, интеграционный — `@Autowired`)

---

## ⚠️ На что обратить внимание

- На диаграмме у `CommunicationSatellite` указано поле `Average Mark` — в текстовом описании задания оно нигде не упоминается, поэтому я его не реализовывал. Если это было намеренно — скажи, добавлю.
- На диаграмме у `ConstellationRepository` другие имена методов (`addConstellation`, `getConstellation`, `getAllConstellations`, `containsConstellation`, `removeConstellation`) — я оставил уже существующие `save`/`findByName`/`findAll`/`existsByName`/`deleteByName` из семинара 3, чтобы не ломать сервис и тесты без явного запроса на переименование.
- Пункт 3 задания (Builder для добавления спутников в группировку, фабрика для группировок) — помечен как опциональный, я его не делал. Скажи, если нужно — добавлю отдельным куском.

---

## 🚀 Запуск

```bash
./gradlew bootRun
./gradlew test jacocoTestReport
```

Как и раньше: бизнес-логика (включая весь новый код `EnergySystem`/`SatelliteFactory`) проверена вручную через «развёрнутый» эквивалент Lombok-кода на `javac` — 22/22 сценария прошли, числа заряда побитово совпадают с прошлыми семинарами. Сам Gradle-билд с реальным Lombok/JUnit/Mockito не прогонялся вживую (нет доступа к Maven Central в этой среде) — обязательно прогони `./gradlew test` сам перед сдачей.

---

## 📚 Технологии

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?logo=springboot)
![Lombok](https://img.shields.io/badge/Lombok-enabled-red)
![GoF](https://img.shields.io/badge/patterns-Factory%20Method%20%2B%20Builder-blueviolet)
