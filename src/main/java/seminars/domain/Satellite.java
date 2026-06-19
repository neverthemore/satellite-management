package seminars.domain;

/**
 * Базовый абстрактный класс спутника.
 *
 * Это доменный класс — Spring им НЕ управляет и не создаёт его как бин.
 * Конкретные спутники создаются вручную через конструктор (new),
 * так как у каждого экземпляра свои параметры (имя, заряд, характеристики),
 * что не подходит под модель Spring-синглтонов.
 *
 * SRP: состояние и энергия вынесены в SatelliteState и EnergySystem.
 * OCP: новые типы спутников добавляются через наследование, без изменения этого класса.
 * LSP: любой наследник можно подставить вместо Satellite без нарушения контракта.
 */
public abstract class Satellite {
    protected String name;
    protected SatelliteState state;
    protected EnergySystem energy;

    public Satellite(String name, double batteryLevel) {
        this.name = name;
        this.state = new SatelliteState();
        this.energy = new EnergySystem(batteryLevel);
        System.out.println("Создан спутник: " + name + " (" + batteryLevel + ")");
    }

    /** Включение спутника — разрешено, если заряда достаточно. */
    public boolean activate() {
        if (energy.hasEnoughCharge()) {
            state.activate();
            return true;
        }
        return false;
    }

    /** Выключение спутника, только если он был включен. */
    public void deactivate() {
        if (state.isActive()) {
            state.deactivate();
        }
    }

    /** Каждый наследник реализует собственную логику миссии (OCP). */
    protected abstract void performMission();

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return state.isActive();
    }

    public double getBatteryLevel() {
        return energy.getBatteryLevel();
    }

    /** Человекочитаемый статус — используется сервисом для showConstellationStatus(). */
    public String getStatusInfo() {
        return state.toString();
    }
}
