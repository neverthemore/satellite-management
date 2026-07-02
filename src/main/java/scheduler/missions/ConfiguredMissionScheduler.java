package scheduler.missions;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import scheduler.client.SpaceOperationClient;
import scheduler.properties.SpaceCenterProperties;

/**
 * Читает список запланированных миссий из конфигурации и при старте приложения
 * регистрирует каждую из них в планировщике задач Spring (TaskScheduler).
 *
 * TaskScheduler создаётся автоматически при наличии @EnableScheduling на
 * главном классе. schedule(Runnable, CronTrigger) позволяет задать
 * произвольное расписание через cron-выражение без жёсткой привязки к
 * фиксированному интервалу.
 *
 * @PostConstruct гарантирует, что регистрация произойдёт после того, как
 * Spring полностью построит бин (и внедрит все зависимости), но до первого
 * cron-тика — никаких гонок при старте.
 */
@Service
public class ConfiguredMissionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConfiguredMissionScheduler.class);

    private final SpaceCenterProperties properties;
    private final SpaceOperationClient spaceOperationClient;
    private final TaskScheduler taskScheduler;

    public ConfiguredMissionScheduler(
            SpaceCenterProperties properties,
            SpaceOperationClient spaceOperationClient,
            TaskScheduler taskScheduler) {
        this.properties = properties;
        this.spaceOperationClient = spaceOperationClient;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void scheduleMissions() {
        if (properties.missions() == null || properties.missions().isEmpty()) {
            log.warn("Список миссий в конфигурации пуст — планировщик бездействует");
            return;
        }

        for (SpaceCenterProperties.ScheduledMissionConfig mission : properties.missions()) {
            validateMissionConfig(mission);
            scheduleOneMission(mission);
        }

        log.info("Зарегистрировано {} миссий в планировщике", properties.missions().size());
    }

    private void scheduleOneMission(SpaceCenterProperties.ScheduledMissionConfig mission) {
        Runnable task = buildMissionTask(mission);
        CronTrigger cronTrigger = new CronTrigger(mission.cron());

        taskScheduler.schedule(task, cronTrigger);

        log.info("Зарегистрирована миссия: тип={}, группировка='{}', спутник='{}', cron='{}'",
                mission.targetType(),
                mission.constellationName(),
                mission.satelliteName() != null ? mission.satelliteName() : "—",
                mission.cron());
    }

    private Runnable buildMissionTask(SpaceCenterProperties.ScheduledMissionConfig mission) {
        return () -> {
            log.info("▶ Запуск миссии: тип={}, группировка='{}'",
                    mission.targetType(), mission.constellationName());
            try {
                executeMission(mission);
            } catch (Exception e) {
                // Ловим всё — одна упавшая задача не должна остановить весь планировщик
                log.error("✘ Критическая ошибка при выполнении миссии для '{}': {}",
                        mission.constellationName(), e.getMessage(), e);
            }
        };
    }

    private void executeMission(SpaceCenterProperties.ScheduledMissionConfig mission) {
        switch (mission.targetType()) {
            case CONSTELLATION -> {
                MissionRequest request = MissionRequest.forConstellation(mission.constellationName());
                boolean success = spaceOperationClient.executeMission(request);
                if (success) {
                    log.info("✔ Миссия группировки '{}' выполнена", mission.constellationName());
                } else {
                    log.warn("✘ Миссия группировки '{}' не удалась — основной сервис недоступен или вернул ошибку",
                            mission.constellationName());
                }
            }
            case SINGLE_SATELLITE -> {
                // Для единственного спутника отправляем миссию всей группировки —
                // контроллер основного сервиса принимает MissionRequest с constellationName,
                // фильтрацию по спутнику можно добавить позже через отдельный эндпоинт
                log.info("Запуск миссии для спутника '{}' в группировке '{}'",
                        mission.satelliteName(), mission.constellationName());
                MissionRequest request = MissionRequest.forConstellation(mission.constellationName());
                boolean success = spaceOperationClient.executeMission(request);
                if (success) {
                    log.info("✔ Миссия для спутника '{}' выполнена", mission.satelliteName());
                } else {
                    log.warn("✘ Миссия для спутника '{}' не удалась", mission.satelliteName());
                }
            }
        }
    }

    private void validateMissionConfig(SpaceCenterProperties.ScheduledMissionConfig mission) {
        if (mission.constellationName() == null || mission.constellationName().isBlank()) {
            throw new IllegalStateException("Миссия без constellationName недопустима: " + mission);
        }
        if (mission.cron() == null || mission.cron().isBlank()) {
            throw new IllegalStateException("Миссия без cron-выражения недопустима: " + mission);
        }
        if (mission.targetType() == SpaceCenterProperties.TargetType.SINGLE_SATELLITE
                && (mission.satelliteName() == null || mission.satelliteName().isBlank())) {
            throw new IllegalStateException(
                    "Миссия типа SINGLE_SATELLITE требует satelliteName: " + mission);
        }
    }
}
