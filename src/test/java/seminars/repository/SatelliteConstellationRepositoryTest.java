package seminars.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import seminars.domain.CommunicationSatellite;
import seminars.domain.ImagingSatellite;
import seminars.domain.Satellite;
import seminars.domain.SatelliteConstellation;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JPA-тесты репозиториев на H2 in-memory.
 *
 * @DataJpaTest поднимает только JPA-слой (сущности, репозитории, H2),
 * без Spring MVC, AOP, фабрик и т.д. — тесты изолированы и быстры.
 * @ActiveProfiles("test") подключает application-test.yaml:
 *   flyway.enabled=false, ddl-auto=create-drop, H2 dialect.
 *
 * В реальном проекте для интеграционных тестов лучше использовать
 * Testcontainers с реальным PostgreSQL, чтобы проверять PostgreSQL-специфику.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SatelliteConstellationRepository: JPA-тесты")
class SatelliteConstellationRepositoryTest {

    private static final String CONSTELLATION_NAME = "JPA-Орбита-1";
    private static final String UNKNOWN_CONSTELLATION_NAME = "Несуществующая-JPA";

    private static final String COMM_SAT_NAME = "JPA-Связь-1";
    private static final String IMG_SAT_NAME = "JPA-ДЗЗ-1";

    @Autowired
    private SatelliteConstellationRepository constellationRepository;

    @Autowired
    private SatelliteRepository satelliteRepository;

    private SatelliteConstellation savedConstellation;

    @BeforeEach
    void setUp() {
        SatelliteConstellation constellation = new SatelliteConstellation(CONSTELLATION_NAME);
        constellation.addSatellite(new CommunicationSatellite(COMM_SAT_NAME, 0.85, 500.0));
        constellation.addSatellite(new ImagingSatellite(IMG_SAT_NAME, 0.92, 2.5));
        savedConstellation = constellationRepository.save(constellation);
    }

    @Test
    @DisplayName("save() сохраняет группировку с каскадным сохранением спутников")
    void save_constellationWithSatellites_persistsBothInDatabase() {
        assertNotNull(savedConstellation.getId());
        assertEquals(2, satelliteRepository.count());
    }

    @Test
    @DisplayName("findByConstellationName() возвращает сохранённую группировку")
    void findByConstellationName_existingName_returnsConstellation() {
        Optional<SatelliteConstellation> found =
                constellationRepository.findByConstellationName(CONSTELLATION_NAME);

        assertTrue(found.isPresent());
        assertEquals(CONSTELLATION_NAME, found.get().getConstellationName());
    }

    @Test
    @DisplayName("findByConstellationName() возвращает пустой Optional для несуществующей группировки")
    void findByConstellationName_unknownName_returnsEmpty() {
        Optional<SatelliteConstellation> found =
                constellationRepository.findByConstellationName(UNKNOWN_CONSTELLATION_NAME);

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("findByConstellationNameWithSatellites() загружает спутники в том же запросе (JOIN FETCH)")
    void findByConstellationNameWithSatellites_loadsConstellationAndSatellitesTogether() {
        Optional<SatelliteConstellation> found =
                constellationRepository.findByConstellationNameWithSatellites(CONSTELLATION_NAME);

        assertTrue(found.isPresent());
        assertEquals(2, found.get().getSatellites().size());

        List<String> names = found.get().getSatellites().stream()
                .map(Satellite::getName).toList();
        assertTrue(names.contains(COMM_SAT_NAME));
        assertTrue(names.contains(IMG_SAT_NAME));
    }

    @Test
    @DisplayName("existsByConstellationName() возвращает true для сохранённой и false для несуществующей")
    void existsByConstellationName_returnsCorrectBooleans() {
        assertTrue(constellationRepository.existsByConstellationName(CONSTELLATION_NAME));
        assertFalse(constellationRepository.existsByConstellationName(UNKNOWN_CONSTELLATION_NAME));
    }

    @Test
    @DisplayName("deleteByConstellationName() удаляет группировку и каскадно удаляет спутников")
    void deleteByConstellationName_removesConstellationAndCascadesSpawnToSatellites() {
        constellationRepository.deleteByConstellationName(CONSTELLATION_NAME);

        assertFalse(constellationRepository.existsByConstellationName(CONSTELLATION_NAME));
        assertEquals(0, satelliteRepository.count());
    }

    @Test
    @DisplayName("Активация спутника сохраняется в БД (@Embedded dirty checking)")
    void activateSatellite_embeddedStateFieldsArePersisted() {
        SatelliteConstellation constellation = constellationRepository
                .findByConstellationNameWithSatellites(CONSTELLATION_NAME).orElseThrow();

        Satellite satellite = constellation.getSatellites().get(0);
        satellite.activate();
        constellationRepository.save(constellation);

        // Перечитываем из БД — dirty checking должен был сохранить is_active = true
        Satellite reloaded = satelliteRepository.findByName(satellite.getName()).orElseThrow();
        assertTrue(reloaded.isActive());
        assertTrue(reloaded.getBatteryLevel() > 0);
    }
}
