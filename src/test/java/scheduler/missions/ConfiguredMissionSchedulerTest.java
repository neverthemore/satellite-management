package scheduler.missions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import scheduler.client.SpaceOperationClient;
import scheduler.properties.SpaceCenterProperties;
import scheduler.properties.SpaceCenterProperties.ScheduledMissionConfig;
import scheduler.properties.SpaceCenterProperties.TargetType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Юнит-тест ConfiguredMissionScheduler.
 *
 * Проверяет, что scheduleMissions() корректно регистрирует задачи в
 * TaskScheduler (по одной на каждую миссию из конфига) и что валидация
 * конфигурации работает — всё без поднятия Spring-контекста.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfiguredMissionScheduler: юнит-тесты регистрации миссий")
class ConfiguredMissionSchedulerTest {

    private static final String VALID_CRON = "0 */1 * * * *";
    private static final String CONSTELLATION_NAME = "Test-Constellation";
    private static final String SATELLITE_NAME = "Test-Sat-1";

    @Mock
    private SpaceOperationClient spaceOperationClient;

    @Mock
    private TaskScheduler taskScheduler;

    private ConfiguredMissionScheduler scheduler;

    @BeforeEach
    void setUp() {
    }

    private ConfiguredMissionScheduler buildScheduler(SpaceCenterProperties properties) {
        return new ConfiguredMissionScheduler(properties, spaceOperationClient, taskScheduler);
    }

    @Test
    @DisplayName("scheduleMissions() регистрирует по одной задаче в TaskScheduler на каждую миссию из конфига")
    void scheduleMissions_twoDeclaredMissions_registersEachInTaskScheduler() {
        SpaceCenterProperties properties = new SpaceCenterProperties(
                "http://localhost:8080/api",
                List.of(
                        new ScheduledMissionConfig(TargetType.CONSTELLATION, CONSTELLATION_NAME, null, VALID_CRON),
                        new ScheduledMissionConfig(TargetType.CONSTELLATION, "Другая-Группировка", null, VALID_CRON)
                )
        );
        scheduler = buildScheduler(properties);

        scheduler.scheduleMissions();

        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    @DisplayName("scheduleMissions() при пустом списке не регистрирует ни одной задачи")
    void scheduleMissions_emptyMissionList_doesNotRegisterAnyTask() {
        SpaceCenterProperties properties = new SpaceCenterProperties("http://localhost:8080/api", List.of());
        scheduler = buildScheduler(properties);

        scheduler.scheduleMissions();

        verifyNoInteractions(taskScheduler);
    }

    @Test
    @DisplayName("Валидация: SINGLE_SATELLITE без satelliteName выбрасывает IllegalStateException")
    void scheduleMissions_singleSatelliteWithoutSatelliteName_throwsIllegalStateException() {
        SpaceCenterProperties properties = new SpaceCenterProperties(
                "http://localhost:8080/api",
                List.of(new ScheduledMissionConfig(TargetType.SINGLE_SATELLITE, CONSTELLATION_NAME, null, VALID_CRON))
        );
        scheduler = buildScheduler(properties);

        assertThrows(IllegalStateException.class, () -> scheduler.scheduleMissions());
    }

    @Test
    @DisplayName("Валидация: SINGLE_SATELLITE с satelliteName успешно регистрируется")
    void scheduleMissions_singleSatelliteWithSatelliteName_registersSuccessfully() {
        SpaceCenterProperties properties = new SpaceCenterProperties(
                "http://localhost:8080/api",
                List.of(new ScheduledMissionConfig(TargetType.SINGLE_SATELLITE, CONSTELLATION_NAME, SATELLITE_NAME, VALID_CRON))
        );
        scheduler = buildScheduler(properties);

        assertDoesNotThrow(() -> scheduler.scheduleMissions());
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));
    }
}
