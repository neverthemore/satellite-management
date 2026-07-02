package seminars.domain;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)   // если есть наследники
public abstract class Satellite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    protected String name;
    protected boolean isActive;
    protected double batteryLevel;

    // Пустой конструктор (обязателен для JPA)
    protected Satellite() {
    }

    public Satellite(String name, double batteryLevel) {
        this.name = name;
        this.batteryLevel = batteryLevel;
        this.isActive = false;
        System.out.println("Создан спутник: " + name + " (заряд: " + (int)(batteryLevel * 100) + "%)");
    }

    public boolean activate() {
        if (batteryLevel > 0.2) {
            isActive = true;
            return true;
        }
        return false;
    }

    public void deactivate() {
        if (isActive) {
            isActive = false;
        }
    }

    public void consumeBattery(double amount) {
        batteryLevel -= amount;
        if (batteryLevel <= 0.2) {
            deactivate();
        }
    }

    protected abstract void performMission();

    // Геттеры
    public String getName() { return name; }
    public boolean isActive() { return isActive; }
    public double getBatteryLevel() { return batteryLevel; }
    public Long getId() { return id; }

    // Сеттер для id (нужен Hibernate)
    public void setId(Long id) { this.id = id; }
}