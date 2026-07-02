package seminars.factory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class CommunicationSatelliteParam extends SatelliteParam {
    private final double bandwidth;

    @JsonCreator
    public CommunicationSatelliteParam(
            @JsonProperty("name") String name,
            @JsonProperty("batteryLevel") double batteryLevel,
            @JsonProperty("bandwidth") double bandwidth) {
        super(SatelliteType.COMMUNICATION, name, batteryLevel);
        this.bandwidth = bandwidth;
    }
}
