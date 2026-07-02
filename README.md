# 🛰️ Satellite Management System — Семинар 7 (Facade + Decorator/AOP)

Сервисный слой упрощён через **Facade**, а замер времени выполнения методов реализован через **Decorator** в виде Spring AOP прокси (кастомная аннотация + аспект).

---

## 📋 Что изменилось по сравнению с семинаром 6

| Было | Стало |
|---|---|
| `SpaceOperationCenterService` (в `seminars.service`) работал только с группировками | Переименован в `ConstellationService`, плюс 3 новых метода (`existsConstellation`, `getConstellation`, `deactivateAllSatellites`) |
| Вызывающий код сам дёргал `ConstellationService` и `SatelliteService` в правильном порядке | Новый `SpaceOperationCenterService` (теперь в `seminars.facade`) — **фасад**, агрегирующий оба сервиса |
| — | `@LogExecutionTime` + `ExecutionTimeAspect` — кастомный декоратор через Spring AOP прокси, замеряет время выполнения методов |

---

## 🗂️ Структура проекта

```
satellite-spring/
└── src/main/java/seminars/
    ├── Main.java                         # теперь работает в основном через фасад
    ├── aop/                              # НОВЫЙ пакет
    │   ├── LogExecutionTime.java         # кастомная аннотация (@Retention RUNTIME)
    │   └── ExecutionTimeAspect.java      # @Aspect, @Around — сам декоратор
    ├── facade/                           # НОВЫЙ пакет
    │   ├── SpaceOperationCenterService.java   # ФАСАД
    │   ├── AddSatelliteRequest.java
    │   ├── MissionRequest.java
    │   └── ConstellationStatusReport.java
    ├── service/
    │   ├── ConstellationService.java     # ПЕРЕИМЕНОВАН из SpaceOperationCenterService
    │   ├── SatelliteService.java
    │   └── SatelliteServiceImpl.java     # + @LogExecutionTime на createSatellite()
    ├── domain/, factory/, exception/, repository/   (без изменений)
└── src/test/java/seminars/
    ├── facade/SpaceOperationCenterServiceTest.java   # НОВЫЙ
    ├── aop/ExecutionTimeAspectTest.java               # НОВЫЙ
    ├── repository/ConstellationRepositoryMockTest.java        # обновлён под ConstellationService
    ├── repository/ConstellationRepositoryIntegrationTest.java # обновлён под ConstellationService
    └── ... (остальные без изменений)
```

---

## 🎭 Facade

```java
@Service
@RequiredArgsConstructor
public class SpaceOperationCenterService {
    private final ConstellationService constellationService;
    private final SatelliteService satelliteService;

    public List<Satellite> addSatellite(AddSatelliteRequest request) { ... }
    public void executeMission(MissionRequest request) { ... }
    public List<Satellite> deployConstellation(AddSatelliteRequest request) { ... }   // доп. метод
    public ConstellationStatusReport getConstellationReport(String name) { ... }       // доп. метод
}
```

**До фасада** (то, что раньше требовалось от вызывающего кода):
```java
if (!constellationService.existsConstellation(name)) {
    constellationService.createAndSaveConstellation(name);
}
Satellite s1 = satelliteService.createSatellite(param1);
constellationService.addSatelliteToConstellation(name, s1);
Satellite s2 = satelliteService.createSatellite(param2);
constellationService.addSatelliteToConstellation(name, s2);
constellationService.activateAllSatellites(name);
constellationService.executeConstellationMission(name);
```

**После фасада:**
```java
operationCenter.deployConstellation(AddSatelliteRequest.builder()
        .constellationName(name)
        .satelliteParam(param1)
        .satelliteParam(param2)
        .build());
```

`AddSatelliteRequest` переиспользует иерархию `SatelliteParam` из семинара 6 (как и предлагалось в задании), `@Singular` от Lombok даёt две формы сборки списка: по одному элементу (`.satelliteParam(x)`) или сразу списком (`.satelliteParams(list)`).

Помимо двух обязательных методов (`addSatellite`, `executeMission`) добавлены два своих:
- **`deployConstellation`** — полный цикл (создание → активация → выполнение миссий) одним вызовом
- **`getConstellationReport`** — сводка по группировке (всего/активных спутников) без необходимости вызывающему коду самому считать активные спутники

---

## ⏱ Decorator через Spring AOP

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecutionTime { }
```

```java
@Aspect
@Component
public class ExecutionTimeAspect {
    @Around("@annotation(seminars.aop.LogExecutionTime)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNanos = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
            System.out.println("⏱ " + joinPoint.getSignature().toShortString() + " выполнен за " + elapsedMillis + " мс");
        }
    }
}
```

Аннотация навешана на `SatelliteServiceImpl.createSatellite()`, `ConstellationService.executeConstellationMission()`, `SpaceOperationCenterService.addSatellite()` и `executeMission()` — то есть видна работа аспекта и на уровне фасада, и на уровне нижних сервисов.

**Важный нюанс (self-invocation):** Spring AOP — proxy-based, перехватываются только вызовы СНАРУЖИ бина. `deployConstellation()` вызывает `this.addSatellite(...)` и `this.executeMission(...)` напрямую (через `this`, в обход прокси) — для ЭТИХ конкретных вложенных вызовов аспект не сработает, хотя сам внешний `deployConstellation()` остаётся неаннотированным. Зато `addSatellite()` корректно перехватывается, когда `Main` зовёт его напрямую, и `satelliteService.createSatellite(...)`, вызванный ИЗ фасада — это уже вызов на ДРУГОЙ бин, такой вызов проходит через прокси `SatelliteServiceImpl` без проблем. Это одна из самых частых AOP-ловушек в проде, полезно знать о ней заранее.

---

## 🧪 Тесты

- **`SpaceOperationCenterServiceTest`** — 6 интеграционных тестов фасада: создание+добавление, переиспользование существующей группировки, `executeMission` с активацией по умолчанию, полный `deployConstellation`, отчёт до/после активации, исключение для неизвестной группировки
- **`ExecutionTimeAspectTest`** — перехватывает `System.out`, проверяет, что вызов аннотированного метода реально печатает строку с замером, что замер не дублируется, и что неаннотированный вызов (`toString()`) ничего не печатает
- `ConstellationRepositoryMockTest`/`ConstellationRepositoryIntegrationTest` обновлены под новое имя `ConstellationService`

---

## 🚀 Запуск

```bash
./gradlew bootRun
./gradlew test jacocoTestReport
```

Как и раньше: вся бизнес-логика фасада проверена вручную через эквивалентный код на `javac` — 14/14 сценариев прошли (оркестрация фасада + отдельно логика измерения времени, включая поведение при исключении). Сам Spring AOP прокси (перехват аннотации, создание CGLIB-обёртки) проверить так нельзя — для этого нужен реальный Spring-контекст, недоступный в этой песочнице (нет доступа к Maven Central). Обязательно прогони `./gradlew test` сам — `ExecutionTimeAspectTest` как раз и существует, чтобы подтвердить, что аспект реально перехватывает вызовы в живом контексте.

---

## 📚 Технологии

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?logo=springboot)
![Spring AOP](https://img.shields.io/badge/Spring%20AOP-enabled-blue)
![Lombok](https://img.shields.io/badge/Lombok-enabled-red)
![GoF](https://img.shields.io/badge/patterns-Facade%20%2B%20Decorator-blueviolet)
