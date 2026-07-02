package scheduler.missions;

/**
 * DTO для тела POST /api/missions запроса к основному сервису.
 * record — компактная неизменяемая структура, Jackson умеет
 * сериализовывать записи (records) без дополнительных аннотаций
 * начиная с jackson-databind 2.12+, который включён в Spring Boot 3.x.
 */
public record MissionRequest(
        String constellationName,
        boolean activateBeforeMission
) {
    /** Удобный фабричный метод для выполнения миссии для всей группировки. */
    public static MissionRequest forConstellation(String constellationName) {
        return new MissionRequest(constellationName, true);
    }
}
