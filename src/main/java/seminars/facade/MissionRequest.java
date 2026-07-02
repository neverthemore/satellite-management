package seminars.facade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Описывает, для какой группировки нужно выполнить миссии.
 */
@Getter
@Builder
@ToString
public class MissionRequest {
    private final String constellationName;

    @Builder.Default
    private final boolean activateBeforeMission = true;

    @JsonCreator
    public MissionRequest(
            @JsonProperty("constellationName") String constellationName,
            @JsonProperty("activateBeforeMission") boolean activateBeforeMission) {
        this.constellationName = constellationName;
        this.activateBeforeMission = activateBeforeMission;
    }
}
