package telemetry.kafka;

/**
 * Локальная копия события о спутнике.
 *
 * Намеренно дублируем record в каждом сервисе (а не выносим в shared-модуль):
 * — слабая связанность: каждый сервис эволюционирует независимо
 * — если поле исчезнет у продюсера, консьюмер не сломается (Jackson игнорирует лишнее)
 *
 * Имена полей ДОЛЖНЫ совпадать с именами у продюсера (seminars.kafka.SatelliteEvent),
 * потому что продюсер сериализует без type-headers, а мы ожидаем тот же JSON.
 */
public record SatelliteEvent(
        String eventType,
        String satelliteName,
        String constellationName,
        String satelliteType,
        double batteryLevel,
        String timestamp
) {}
