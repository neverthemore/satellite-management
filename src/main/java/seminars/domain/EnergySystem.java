package seminars.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Система управления зарядом батареи спутника — @Embeddable.
 *
 * Выбор @Embedded (не @OneToOne):
 *   • EnergySystem не существует без спутника — нет смысла хранить её отдельно
 *   • Нет потребности в прямом API-доступе к системе энергии
 *   • Поля функционально зависят только от PK спутника — не нарушают 3NF
 *   • Меньше JOIN-ов при чтении = лучше производительность
 *
 * JPA требует беспараметрический конструктор. Здесь он protected —
 * для кода снаружи создание возможно только через Builder.
 */
@Embeddable
@Getter
@ToString
public class EnergySystem {

    public static final double DEFAULT_MAX_BATTERY = 1.0;
    public static final double DEFAULT_MIN_BATTERY = 0.0;
    public static final double DEFAULT_LOW_BATTERY_THRESHOLD = 0.2;

    @Column(name = "battery_level", nullable = false)
    private double batteryLevel;

    @Column(name = "max_battery", nullable = false)
    private double maxBattery;

    @Column(name = "min_battery", nullable = false)
    private double minBattery;

    @Column(name = "low_battery_threshold", nullable = false)
    private double lowBatteryThreshold;

    /** Для JPA — Hibernate требует no-arg конструктор. */
    protected EnergySystem() {
        this.maxBattery = DEFAULT_MAX_BATTERY;
        this.minBattery = DEFAULT_MIN_BATTERY;
        this.lowBatteryThreshold = DEFAULT_LOW_BATTERY_THRESHOLD;
        this.batteryLevel = DEFAULT_MAX_BATTERY;
    }

    @Builder
    private EnergySystem(Double batteryLevel, double maxBattery, double minBattery, double lowBatteryThreshold) {
        this.maxBattery = maxBattery;
        this.minBattery = minBattery;
        this.lowBatteryThreshold = lowBatteryThreshold;
        this.batteryLevel = (batteryLevel != null) ? batteryLevel : maxBattery;
        validate();
    }

    private void validate() {
        if (minBattery > maxBattery) {
            throw new IllegalArgumentException(
                    "minBattery (" + minBattery + ") не может быть больше maxBattery (" + maxBattery + ")");
        }
        if (lowBatteryThreshold < minBattery || lowBatteryThreshold > maxBattery) {
            throw new IllegalArgumentException(
                    "lowBatteryThreshold (" + lowBatteryThreshold + ") должен быть в диапазоне ["
                            + minBattery + ", " + maxBattery + "]");
        }
        if (batteryLevel < minBattery || batteryLevel > maxBattery) {
            throw new IllegalArgumentException(
                    "batteryLevel (" + batteryLevel + ") должен быть в диапазоне ["
                            + minBattery + ", " + maxBattery + "]");
        }
    }

    public boolean consume(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Нельзя потребить отрицательное количество энергии: " + amount);
        }
        double requested = batteryLevel - amount;
        batteryLevel = Math.max(requested, minBattery);
        return requested >= minBattery;
    }

    public boolean recharge(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Нельзя восполнить отрицательное количество энергии: " + amount);
        }
        double requested = batteryLevel + amount;
        batteryLevel = Math.min(requested, maxBattery);
        return requested <= maxBattery;
    }

    public boolean hasSufficientPower() {
        return batteryLevel > lowBatteryThreshold;
    }

    public boolean isCritical() {
        return batteryLevel <= lowBatteryThreshold;
    }
}
