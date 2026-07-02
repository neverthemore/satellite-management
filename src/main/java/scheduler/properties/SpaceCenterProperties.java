package scheduler.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Record для типизированного чтения конфигурации планировщика из application.yml.
 *
 * Spring автоматически заполняет поля из ключей под префиксом
 * "app.space-center-service". record — идеальный выбор: неизменяемый,
 * компактный, не требует геттеров (поля record публичны по умолчанию).
 */
@ConfigurationProperties(prefix = "app.space-center-service")
public record SpaceCenterProperties(
        String url,
        List<ScheduledMissionConfig> missions
) {

    /**
     * Конфигурация одной запланированной миссии из YAML.
     */
    public record ScheduledMissionConfig(
            TargetType targetType,
            String constellationName,
            String satelliteName,   // только для SINGLE_SATELLITE
            String cron
    ) {}

    public enum TargetType {
        CONSTELLATION,
        SINGLE_SATELLITE
    }
}
