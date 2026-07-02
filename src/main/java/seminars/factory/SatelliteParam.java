package seminars.factory;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.ToString;

/**
 * Параметр создания спутника.
 *
 * @JsonTypeInfo/@JsonSubTypes — полиморфная (де)сериализация через Jackson:
 * в JSON добавляется поле "type" (COMMUNICATION / IMAGE), по которому Jackson
 * понимает, в какой конкретный класс разворачивать объект при получении запроса.
 * Без этого @RequestBody SatelliteParam не смог бы десериализоваться, так как
 * abstract-класс не знает, в кого превращаться.
 */
@Getter
@ToString
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CommunicationSatelliteParam.class, name = "COMMUNICATION"),
        @JsonSubTypes.Type(value = ImagingSatelliteParam.class, name = "IMAGE")
})
public abstract class SatelliteParam {
    private final SatelliteType type;
    private final String name;
    private final double batteryLevel;

    protected SatelliteParam(SatelliteType type, String name, double batteryLevel) {
        this.type = type;
        this.name = name;
        this.batteryLevel = batteryLevel;
    }
}
