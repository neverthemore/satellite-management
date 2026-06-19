/**
 * Базовый абстрактный класс спутника.
 *
 * SRP:  состояние и энергия вынесены в отдельные классы (SatelliteState, EnergySystem),
 *       Satellite больше не управляет ими напрямую, а делегирует операции.
 * OCP:  новые типы спутников добавляются через наследование и переопределение
 *       performMission(), без изменения этого класса.
 * LSP:  любой наследник можно подставить вместо Satellite — контракт методов
 *       (activate/deactivate/performMission) не нарушается ни в одном наследнике.
 */
public abstract class Satellite {
    protected String name;
    protected SatelliteState state;
    protected EnergySystem energy;

    public Satellite(String name, double batteryLevel) {
        this.name = name;
        this.state = new SatelliteState();
        this.energy = new EnergySystem(batteryLevel);
        System.out.println("Создан спутник: " + name + " (заряд: " + (int) (batteryLevel * 100) + "%)");
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

    /**
     * Метод выполнения миссии. Каждый наследник обязан реализовать
     * собственную логику миссии (OCP — расширение без модификации базового класса).
     */
    protected abstract void performMission();

    public String getName() {
        return name;
    }

    /** Делегирование состояния — внешний код не знает о SatelliteState напрямую. */
    public boolean isActive() {
        return state.isActive();
    }

    /** Делегирование энергии — внешний код не знает о EnergySystem напрямую. */
    public double getBatteryLevel() {
        return energy.getBatteryLevel();
    }
}
