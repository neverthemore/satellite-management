package scheduler.kafka;

/** Локальная копия события о спутнике (намеренное дублирование для слабой связанности). */
public record SatelliteEvent(
        String eventType,
        String satelliteName,
        String constellationName,
        String satelliteType,
        double batteryLevel,
        String timestamp
) {}
