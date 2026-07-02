package seminars.kafka;

import java.time.Instant;

/**
 * Доменное событие, публикуемое в Kafka при создании/удалении спутника.
 *
 * Используем Java record — неизменяемый, компактный, без Lombok-бойлерплейта.
 * Jackson + Spring Boot 3.x (ParameterNamesModule) умеет сериализовать/
 * десериализовать records без дополнительных аннотаций.
 *
 * Одно событие для всех типов изменений — eventType как дискриминатор.
 * Альтернатива: отдельные топики satellite.created / satellite.deleted.
 * Здесь выбран единый топик «satellite-events» — проще для начала,
 * легко расширить потом (добавить новый eventType без нового топика).
 */
public record SatelliteEvent(
        String eventType,          // SATELLITE_CREATED | SATELLITE_DELETED
        String satelliteName,
        String constellationName,
        String satelliteType,      // COMMUNICATION | IMAGE — null при удалении
        double batteryLevel,
        String timestamp           // ISO-8601, UTC
) {

    public static SatelliteEvent created(String satelliteName, String constellationName,
                                         String satelliteType, double batteryLevel) {
        return new SatelliteEvent(
                "SATELLITE_CREATED", satelliteName, constellationName,
                satelliteType, batteryLevel, Instant.now().toString());
    }

    public static SatelliteEvent deleted(String satelliteName, String constellationName) {
        return new SatelliteEvent(
                "SATELLITE_DELETED", satelliteName, constellationName,
                null, 0.0, Instant.now().toString());
    }
}
