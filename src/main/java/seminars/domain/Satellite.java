package seminars.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.ToString;

/**
 * Базовый JPA-класс спутника.
 *
 * Стратегия JOINED:
 *   • Общие поля (name, state, energy) → таблица satellites
 *   • Специфичные поля → communication_satellites / imaging_satellites
 *   • Нет дублирования данных → 3NF соблюдена
 *   • Компромисс: SELECT делает JOIN, но данных мало, индексы помогают
 *
 * @Embedded для EnergySystem и SatelliteState — они не существуют без
 * спутника, нет потребности в отдельном репозитории, не нарушают 3NF.
 */
@Entity
@Table(name = "satellites")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "satellite_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@ToString(exclude = "constellation")
public abstract class Satellite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    protected String name;

    @Embedded
    protected SatelliteState state;

    @Embedded
    protected EnergySystem energy;

    /**
     * Обратная сторона @OneToMany в SatelliteConstellation.
     * @JsonBackReference — исключаем из сериализации, чтобы не было
     * кольцевой ссылки (constellation → satellites → constellation → ...).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constellation_id")
    @JsonBackReference
    private SatelliteConstellation constellation;

    /** Для JPA. */
    protected Satellite() {
        this.state = new SatelliteState();
        this.energy = EnergySystem.builder().build();
    }

    // ── Поля телеметрии (заполняются из gRPC-стрима satellite-telemetry) ──

    /** Внутренняя температура отсеков, °C. Норма: 15–35. Null до первого обновления. */
    @Column(name = "internal_temperature")
    private Double internalTemperature;

    /** Внешняя температура корпуса, °C. В тени: -150, на солнце: +120. */
    @Column(name = "external_temperature")
    private Double externalTemperature;

    /**
     * Обновляет данные телеметрии, пришедшие из gRPC-стрима.
     * Используется в TelemetryGrpcClient при каждом TelemetryUpdate.
     */
    public void updateTelemetry(double internalTemperature, double externalTemperature) {
        this.internalTemperature = internalTemperature;
        this.externalTemperature = externalTemperature;
    }

    protected Satellite(String name, double batteryLevel) {
        this.name = name;
        this.state = new SatelliteState();
        this.energy = EnergySystem.builder().batteryLevel(batteryLevel).build();
        System.out.println("Создан спутник: " + name + " (" + batteryLevel + ")");
    }

    /** Внутренний setter для управления двунаправленной связью. */
    void setConstellation(SatelliteConstellation constellation) {
        this.constellation = constellation;
    }

    public boolean activate() {
        if (energy.hasSufficientPower()) {
            state.activate();
            return true;
        }
        return false;
    }

    public void deactivate() {
        if (state.isActive()) {
            state.deactivate();
        }
    }

    protected abstract void performMission();

    public boolean isActive() {
        return state.isActive();
    }

    public double getBatteryLevel() {
        return energy.getBatteryLevel();
    }

    public String getStatusInfo() {
        return state.toString();
    }
}
