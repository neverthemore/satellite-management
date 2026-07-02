package seminars.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import seminars.kafka.TestKafkaConfig;
import org.springframework.test.context.ActiveProfiles;
import seminars.Main;
import seminars.domain.ImagingSatellite;
import seminars.domain.Satellite;
import seminars.domain.SatelliteConstellation;
import seminars.factory.CommunicationSatelliteParam;
import seminars.factory.ImagingSatelliteParam;
import seminars.service.ConstellationService;
import seminars.service.SatelliteService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционные тесты ConstellationService с реальным Spring-контекстом
 * и H2 in-memory БД (profile "test").
 *
 * Проверяется полный жизненный цикл: создание группировки → добавление
 * спутников (JPA cascade) → активация (dirty checking) → выполнение миссий
 * (изменение battery_level, photos_taken сохраняется).
 */
@SpringBootTest(classes = Main.class)
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@DisplayName("ConstellationService: интеграционные тесты с H2 (JPA)")
class ConstellationRepositoryIntegrationTest {

    private static final String LIFECYCLE_CONSTELLATION = "Интеграция-ЖЦ";
    private static final String SHARED_CONSTELLATION    = "Интеграция-Общий";
    private static final String UNKNOWN_CONSTELLATION   = "Интеграция-Неизвестная";

    private static final String COMM_SAT_NAME = "Связь-Инт";
    private static final String IMG_SAT_NAME  = "ДЗЗ-Инт";

    @Autowired
    private ConstellationService constellationService;

    @Autowired
    private SatelliteService satelliteService;

    @Autowired
    private SatelliteConstellationRepository constellationRepository;

    @Test
    @DisplayName("Полный жизненный цикл: создание → добавление → активация → выполнение миссий")
    void fullLifecycle_jpaStatePersistedBetweenOperations() {
        // 1. Создание группировки
        constellationService.createAndSaveConstellation(LIFECYCLE_CONSTELLATION);
        assertTrue(constellationService.existsConstellation(LIFECYCLE_CONSTELLATION));

        // 2. Создание и добавление спутников через фабрику
        Satellite commSat = satelliteService.createSatellite(
                new CommunicationSatelliteParam(COMM_SAT_NAME, 0.8, 500.0));
        ImagingSatellite imgSat = (ImagingSatellite) satelliteService.createSatellite(
                new ImagingSatelliteParam(IMG_SAT_NAME, 0.9, 2.5));

        constellationService.addSatelliteToConstellation(LIFECYCLE_CONSTELLATION, commSat);
        constellationService.addSatelliteToConstellation(LIFECYCLE_CONSTELLATION, imgSat);

        Optional<SatelliteConstellation> afterAdd =
                constellationService.getConstellation(LIFECYCLE_CONSTELLATION);
        assertTrue(afterAdd.isPresent());
        assertEquals(2, afterAdd.get().getSatellites().size());
        assertFalse(commSat.isActive());

        // 3. Активация
        constellationService.activateAllSatellites(LIFECYCLE_CONSTELLATION);
        assertTrue(commSat.isActive());
        assertTrue(imgSat.isActive());

        // 4. Выполнение миссий (изменения персистируются через @Transactional)
        double batteryBefore = commSat.getBatteryLevel();
        constellationService.executeConstellationMission(LIFECYCLE_CONSTELLATION);
        assertTrue(commSat.getBatteryLevel() < batteryBefore);
        assertEquals(1, imgSat.getPhotosTaken());
    }

    @Test
    @DisplayName("createAndSaveConstellation() и getConstellation() используют один и тот же JPA-бин")
    void savedConstellation_visibleThroughRepository() {
        SatelliteConstellation saved =
                constellationService.createAndSaveConstellation(SHARED_CONSTELLATION);

        Optional<SatelliteConstellation> found =
                constellationRepository.findByConstellationName(SHARED_CONSTELLATION);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("addSatelliteToConstellation() для несуществующей группировки бросает исключение")
    void addSatelliteToUnknownConstellation_throwsIllegalArgumentException() {
        Satellite sat = satelliteService.createSatellite(
                new CommunicationSatelliteParam(COMM_SAT_NAME + "-2", 0.7, 300.0));

        assertThrows(IllegalArgumentException.class,
                () -> constellationService.addSatelliteToConstellation(UNKNOWN_CONSTELLATION, sat));
    }

    @Test
    @DisplayName("getAllConstellations() возвращает List с уже сохранёнными группировками")
    void getAllConstellations_includesPreviouslySavedConstellations() {
        constellationService.createAndSaveConstellation(LIFECYCLE_CONSTELLATION + "-list");

        List<SatelliteConstellation> all = constellationService.getAllConstellations();
        assertTrue(all.stream()
                .anyMatch(c -> c.getConstellationName().equals(LIFECYCLE_CONSTELLATION + "-list")));
    }
}
