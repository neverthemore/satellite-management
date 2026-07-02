package seminars.domain;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Satellite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected String name;
    protected boolean active;            // <-- поле active
    protected double batteryLevel;

    protected Satellite() {
    }

    public Satellite(String name, double batteryLevel) {
        this.name = name;
        this.batteryLevel = batteryLevel;
        this.active = false;             // ✅ используем поле active
        System.out.println("Создан спутник: " + name + " (заряд: " + (int)(batteryLevel * 100) + "%)");
    }

    public boolean activate() {
        if (batteryLevel > 0.2) {
            active = true;               // ✅
            return true;
        }
        return false;
    }

    public void deactivate() {
        if (active) {                    // ✅
            active = false;
        }
    }

    public void consumeBattery(double amount) {
        batteryLevel -= amount;
        if (batteryLevel <= 0.2) {
            deactivate();
        }
    }

    protected abstract void performMission();

    public String getName() { return name; }
    public boolean isActive() { return active; }    // ✅ возвращаем active
    public double getBatteryLevel() { return batteryLevel; }
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }
}