/**
 * Отвечает ТОЛЬКО за управление зарядом батареи спутника.
 * Реализует принцип единственной ответственности (SRP).
 */
public class EnergySystem {
    private static final double CRITICAL_THRESHOLD = 0.2;

    private double batteryLevel;

    public EnergySystem(double initialLevel) {
        this.batteryLevel = initialLevel;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    /** Списывает заданное количество заряда. */
    public void consume(double amount) {
        batteryLevel -= amount;
    }

    /** Достаточно ли заряда для активации/продолжения работы. */
    public boolean hasEnoughCharge() {
        return batteryLevel > CRITICAL_THRESHOLD;
    }

    /** Достиг ли заряд критически низкого уровня. */
    public boolean isCritical() {
        return batteryLevel <= CRITICAL_THRESHOLD;
    }
}
