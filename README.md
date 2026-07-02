# 🛰️ Satellite Management System — Семинар 6 (Strategy + доработка Factory Method)

Единая точка создания спутников через `SatelliteService`, который сам выбирает нужную фабрику среди всех зарегистрированных (паттерн **Strategy**), плюс унификация `SatelliteFactory` через иерархию параметров (паттерн **Command**).

---

## 📋 Что изменилось по сравнению с семинаром 5

| Было | Стало |
|---|---|
| `SatelliteFactory` — абстрактный класс с `createSatellite(name, batteryLevel)` и `createSatelliteWithParameter(name, batteryLevel, parameter)` | `SatelliteFactory` — **интерфейс** с единственным методом `createSatelliteWithParameter(SatelliteParam param)` + `isSatelliteTypeSupported(SatelliteType)` |
| `Main`/тесты напрямую вызывали конкретные фабрики | Всё создание идёт через `SatelliteService.createSatellite(param)` — сервис сам находит подходящую фабрику |
| Параметры передавались как примитивы (`String, double, double`) | Введена иерархия `SatelliteParam` → `ImagingSatelliteParam` / `CommunicationSatelliteParam` |
| — | Новое исключение `SpaceOperationException` — для неподдерживаемого типа параметра и для "фабрика не найдена" |

---

## 🗂️ Структура проекта

```
satellite-spring/
└── src/main/java/seminars/
    ├── Main.java                              # создаёт спутники только через SatelliteService
    ├── exception/
    │   └── SpaceOperationException.java       # НОВЫЙ
    ├── factory/
    │   ├── SatelliteType.java                 # НОВЫЙ: enum IMAGE / COMMUNICATION
    │   ├── SatelliteParam.java                # НОВЫЙ: абстрактный параметр (Command)
    │   ├── ImagingSatelliteParam.java         # НОВЫЙ
    │   ├── CommunicationSatelliteParam.java   # НОВЫЙ
    │   ├── SatelliteFactory.java              # ПЕРЕРАБОТАН: теперь interface
    │   ├── CommunicationSatelliteFactory.java # ПЕРЕРАБОТАН: instanceof + исключение
    │   └── ImagingSatelliteFactory.java       # ПЕРЕРАБОТАН: instanceof + исключение
    ├── domain/  (без изменений)
    ├── repository/ConstellationRepository.java  (без изменений)
    └── service/
        ├── SatelliteService.java              # НОВЫЙ: интерфейс
        ├── SatelliteServiceImpl.java          # НОВЫЙ: Strategy — список фабрик, выбор по типу
        └── SpaceOperationCenterService.java    (без изменений)
└── src/test/java/seminars/
    ├── factory/SatelliteFactoryTest.java       # ПЕРЕПИСАН под SatelliteParam
    ├── service/
    │   ├── SatelliteServiceTest.java          # НОВЫЙ (обязательный): @SpringBootTest
    │   └── SatelliteServiceImplTest.java      # НОВЫЙ (доп.): юнит-тест, граничные случаи
    ├── domain/EnergySystemTest.java            (без изменений)
    └── repository/
        ├── ConstellationRepositoryUnitTest.java          (без изменений)
        ├── ConstellationRepositoryMockTest.java           # спутники теперь через SatelliteParam
        └── ConstellationRepositoryIntegrationTest.java    # @Autowired SatelliteService вместо фабрик
```

---

## 🧩 Как это работает

```java
// Вызывающему коду не нужно знать, какая фабрика будет использована
Satellite satellite = satelliteService.createSatellite(
        new CommunicationSatelliteParam("Связь-1", 0.85, 500.0));
```

**`SatelliteServiceImpl`** (Strategy):
```java
@Service
@RequiredArgsConstructor
public class SatelliteServiceImpl implements SatelliteService {
    private final List<SatelliteFactory> factories;   // Spring сам соберёт все @Component-фабрики

    public Satellite createSatellite(SatelliteParam param) {
        SatelliteFactory factory = factories.stream()
                .filter(f -> f.isSatelliteTypeSupported(param.getType()))
                .findFirst()
                .orElseThrow(() -> new SpaceOperationException(...));
        return factory.createSatelliteWithParameter(param);
    }
}
```

Список `factories` — это и есть набор взаимозаменяемых "стратегий" создания. Чтобы добавить новый тип спутника, достаточно создать новый `SatelliteParam`-наследник и новую `@Component`-фабрику — `SatelliteServiceImpl` трогать не нужно (OCP).

**Конкретная фабрика** проверяет тип параметра через `instanceof` и кидает `SpaceOperationException`, если её попросили создать не "свой" тип:
```java
@Override
public Satellite createSatelliteWithParameter(SatelliteParam param) {
    if (!(param instanceof CommunicationSatelliteParam communicationParam)) {
        throw new SpaceOperationException(...);
    }
    return new CommunicationSatellite(
            communicationParam.getName(), communicationParam.getBatteryLevel(), communicationParam.getBandwidth());
}
```

---

## 🧪 Тесты

- **`SatelliteFactoryTest`** — переписан: создание через `SatelliteParam`, плюс новые сценарии — `isSatelliteTypeSupported()` для обоих типов и `SpaceOperationException` при параметре чужого типа. Тесты на "дефолтные" параметры удалены — такого сценария в новом API больше не существует.
- **`SatelliteServiceTest`** *(обязательный по заданию)* — `@SpringBootTest`, создаёт спутники через реально внедрённый `SatelliteService`, проверяет тип и характеристики результата.
- **`SatelliteServiceImplTest`** *(дополнительно)* — юнит-тест с вручную собранным списком фабрик: проверяет граничные случаи "подходящей фабрики нет" и "список фабрик пуст" — такое невозможно воспроизвести в реальном Spring-контексте, где обе фабрики всегда зарегистрированы.
- `ConstellationRepositoryMockTest` / `ConstellationRepositoryIntegrationTest` обновлены под новый API создания спутников.

---

## 🚀 Запуск

```bash
./gradlew bootRun
./gradlew test jacocoTestReport
```

Как и раньше: вся новая логика (фабрики, сервис, параметры) проверена вручную через «развёрнутый» эквивалент кода на `javac` — 17/17 сценариев прошли, включая happy path обеих фабрик, обе ветки `SpaceOperationException`, выбор стратегии в сервисе и оба граничных случая (фабрика не найдена / список фабрик пуст). Реальный Gradle-билд с Lombok/JUnit/Mockito/Spring не прогонялся вживую (нет доступа к Maven Central в этой среде) — обязательно прогони `./gradlew test` сам перед сдачей.

---

## 📚 Технологии

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?logo=springboot)
![Lombok](https://img.shields.io/badge/Lombok-enabled-red)
![GoF](https://img.shields.io/badge/patterns-Factory%20Method%20%2B%20Strategy%20%2B%20Command-blueviolet)
