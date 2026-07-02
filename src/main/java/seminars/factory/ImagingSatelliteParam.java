package seminars.factory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class ImagingSatelliteParam extends SatelliteParam {
    private final double resolution;

    @JsonCreator
    public ImagingSatelliteParam(
            @JsonProperty("name") String name,
            @JsonProperty("batteryLevel") double batteryLevel,
            @JsonProperty("resolution") double resolution) {
        super(SatelliteType.IMAGE, name, batteryLevel);
        this.resolution = resolution;
    }
}
