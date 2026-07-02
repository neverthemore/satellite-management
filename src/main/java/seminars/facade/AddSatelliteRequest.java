package seminars.facade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import seminars.factory.SatelliteParam;

import java.util.List;

/**
 * Параметры группировки и спутников, которые нужно в неё добавить.
 *
 * @JsonCreator + @JsonProperty — явный конструктор для Jackson, чтобы
 * Lombok @Builder (который прячет конструктор) не мешал десериализации
 * из JSON-тела запроса (@RequestBody в контроллере).
 */
@Getter
@Builder
@ToString
public class AddSatelliteRequest {
    private final String constellationName;

    @Singular
    private final List<SatelliteParam> satelliteParams;

    @JsonCreator
    public AddSatelliteRequest(
            @JsonProperty("constellationName") String constellationName,
            @JsonProperty("satelliteParams") List<SatelliteParam> satelliteParams) {
        this.constellationName = constellationName;
        this.satelliteParams = satelliteParams;
    }
}
