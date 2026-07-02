# 🛰️ Satellite Management System — Семинар 2 (принципы SOLID)

Рефакторинг системы управления спутниковой группировкой под принципы **SRP**, **OCP** и **LSP**.

---

## 📋 Что изменилось по сравнению с семинаром 1

Исходный `Satellite` хранил поля `isActive` и `batteryLevel` напрямую и сам управлял и состоянием, и энергией — нарушая принцип единственной ответственности. Теперь это вынесено в отдельные классы, а `Satellite` работает с ними через **композицию**.

---

## 🗂️ Структура проекта

```
satellite-solid/
└── src/
    ├── EnergySystem.java            # НОВЫЙ — управление зарядом батареи
    ├── SatelliteState.java          # НОВЫЙ — управление статусом активности
    ├── Satellite.java               # Абстрактный класс, теперь через композицию
    ├── CommunicationSatellite.java
    ├── ImagingSatellite.java
    ├── SatelliteConstellation.java  # Не изменился — работает только через публичный API Satellite
    └── Main.java                    # Не изменился
```

---

## ⚙️ Архитектура

| Класс | Роль |
|---|---|
| `EnergySystem` | Только заряд: `consume()`, `hasEnoughCharge()`, `isCritical()` |
| `SatelliteState` | Только статус: `activate()`, `deactivate()`, `isActive()` |
| `Satellite` | Хранит `protected SatelliteState state` и `protected EnergySystem energy`, оркеструет их через `activate()`/`deactivate()` |
| `CommunicationSatellite`/`ImagingSatellite` | Внутри `performMission()` напрямую используют `state.isActive()` и `energy.consume(...)`, сами проверяют `energy.isCritical()` для автовыключения |

---

## ✅ Принципы SOLID

- **SRP** — заряд и состояние теперь в отдельных классах, у каждого одна причина для изменения
- **OCP** — чтобы добавить новый тип спутника, не нужно трогать `Satellite`: достаточно наследоваться и переопределить `performMission()`
- **LSP** — `Main` и `SatelliteConstellation` не потребовали ни одной правки после рефакторинга: они работают только через стабильный публичный контракт `Satellite` (`activate`, `isActive`, `getBatteryLevel`, `getName`), и любой наследник подставляется в это место без сюрпризов

---

## 🚀 Запуск

Требуется Java 17+.

```bash
cd src
javac -encoding UTF-8 *.java
java -Dfile.encoding=UTF-8 Main
```

Вывод в консоли побайтово идентичен семинару 1 — поведение системы сохранено полностью, изменилась только внутренняя архитектура.

---

## 📚 Технологии

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
