package seminars.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.ToString;

@Entity
@Table(name = "imaging_satellites")
@DiscriminatorValue("IMAGE")
@Getter
@ToString(callSuper = true)
public class ImagingSatellite extends Satellite {

    private static final double MISSION_ENERGY_COST = 0.08;

    @Column(nullable = false)
    private double resolution;

    @Column(name = "photos_taken", nullable = false)
    private int photosTaken;

    /** Для JPA. */
    protected ImagingSatellite() {}

    @JsonCreator
    public ImagingSatellite(
            @JsonProperty("name") String name,
            @JsonProperty("batteryLevel") double batteryLevel,
            @JsonProperty("resolution") double resolution) {
        super(name, batteryLevel);
        this.resolution = resolution;
        this.photosTaken = 0;
    }

    @Override
    protected void performMission() {
        if (state.isActive()) {
            System.out.println(name + ": Съемка территории с разрешением " + resolution + " м/пиксель");
            takePhoto();

            boolean fullyConsumed = energy.consume(MISSION_ENERGY_COST);
            if (!fullyConsumed || energy.isCritical()) {
                state.deactivate();
            }
        } else {
            System.out.println("🛑 " + name + ": Не может выполнить съемку - не активен");
        }
    }

    public void takePhoto() {
        if (state.isActive()) {
            photosTaken++;
            System.out.println(name + ": Снимок #" + photosTaken + " сделан!");
        }
    }
}
