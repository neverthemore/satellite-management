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
@Table(name = "communication_satellites")
@DiscriminatorValue("COMMUNICATION")
@Getter
@ToString(callSuper = true)
public class CommunicationSatellite extends Satellite {

    private static final double MISSION_ENERGY_COST = 0.05;

    @Column(nullable = false)
    private double bandwidth;

    /** Для JPA. */
    protected CommunicationSatellite() {}

    @JsonCreator
    public CommunicationSatellite(
            @JsonProperty("name") String name,
            @JsonProperty("batteryLevel") double batteryLevel,
            @JsonProperty("bandwidth") double bandwidth) {
        super(name, batteryLevel);
        this.bandwidth = bandwidth;
    }

    @Override
    protected void performMission() {
        if (state.isActive()) {
            System.out.println(name + ": Передача данных со скоростью " + bandwidth + " Мбит/с");
            sendData(bandwidth);

            boolean fullyConsumed = energy.consume(MISSION_ENERGY_COST);
            if (!fullyConsumed || energy.isCritical()) {
                state.deactivate();
            }
        }
    }

    public void sendData(double amount) {
        if (state.isActive()) {
            System.out.println(name + ": Отправил " + amount + " Мбит данных!");
        }
    }
}
