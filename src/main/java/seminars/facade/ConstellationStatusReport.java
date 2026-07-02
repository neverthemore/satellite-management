package seminars.facade;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Краткая сводка по группировке — вместо того, чтобы вызывающий код сам
 * проходился по списку спутников и считал активные, фасад отдаёт уже
 * готовые цифры.
 */
@Getter
@Builder
@ToString
public class ConstellationStatusReport {
    private final String constellationName;
    private final int totalSatellites;
    private final int activeSatellites;
}
