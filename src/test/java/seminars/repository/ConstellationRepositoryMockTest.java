package seminars.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seminars.domain.Satellite;
import seminars.domain.SatelliteConstellation;
import seminars.factory.CommunicationSatelliteFactory;
import seminars.factory.CommunicationSatelliteParam;
import seminars.kafka.SatelliteEventPublisher;
import seminars.service.ConstellationService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Мок-тест ConstellationService с подменёнными JPA-репозиториями.
 *
 * ConstellationService теперь принимает три зависимости (@RequiredArgsConstructor):
 *   • SatelliteConstellationRepository
 *   • SatelliteRepository
 *   • SatelliteEventPublisher  ← добавлен в семинаре 12 (Kafka)
 *
 * Все три мокаются — Spring-контекст не поднимается.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConstellationService: мок-тесты с подменёнными JPA-репозиториями")
class ConstellationRepositoryMockTest {

    private static final String CONSTELLATION_NAME = "Орбита-Мок";
    private static final String UNKNOWN_CONSTELLATION_NAME = "Несуществующая-Мок";
    private static final String SATELLITE_NAME = "Связь-Мок-1";
    private static final double SATELLITE_BATTERY = 0.9;
    private static final double SATELLITE_BANDWIDTH = 750.0;

    @Mock
    private SatelliteConstellationRepository constellationRepository;

    @Mock
    private SatelliteRepository satelliteRepository;

    @Mock
    private SatelliteEventPublisher satelliteEventPublisher;

    @InjectMocks
    private ConstellationService constellationService;

    private final CommunicationSatelliteFactory factory = new CommunicationSatelliteFactory();

    private SatelliteConstellation constellation;
    private Satellite satellite;

    @BeforeEach
    void setUp() {
        constellation = new SatelliteConstellation(CONSTELLATION_NAME);
        satellite = factory.createSatelliteWithParameter(
                new CommunicationSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY, SATELLITE_BANDWIDTH));
    }

    @Test
    @DisplayName("createAndSaveConstellation() делегирует save() в репозиторий")
    void createAndSaveConstellation_delegatesToRepositorySave() {
        when(constellationRepository.save(any(SatelliteConstellation.class))).thenReturn(constellation);

        constellationService.createAndSaveConstellation(CONSTELLATION_NAME);

        verify(constellationRepository, times(1)).save(any(SatelliteConstellation.class));
    }

    @Test
    @DisplayName("addSatelliteToConstellation() находит группировку и сохраняет с новым спутником")
    void addSatelliteToConstellation_existingConstellation_savesWithSatellite() {
        when(constellationRepository.findByConstellationNameWithSatellites(CONSTELLATION_NAME))
                .thenReturn(Optional.of(constellation));
        when(constellationRepository.save(any(SatelliteConstellation.class))).thenReturn(constellation);

        constellationService.addSatelliteToConstellation(CONSTELLATION_NAME, satellite);

        assertTrue(constellation.getSatellites().contains(satellite));
        verify(constellationRepository, times(1)).save(constellation);
    }

    @Test
    @DisplayName("addSatelliteToConstellation() для несуществующей группировки бросает IllegalArgumentException")
    void addSatelliteToConstellation_unknownConstellation_throwsException() {
        when(constellationRepository.findByConstellationNameWithSatellites(UNKNOWN_CONSTELLATION_NAME))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> constellationService.addSatelliteToConstellation(UNKNOWN_CONSTELLATION_NAME, satellite));
    }

    @Test
    @DisplayName("[Kafka] addSatelliteToConstellation() публикует SATELLITE_CREATED событие после сохранения")
    void addSatelliteToConstellation_existingConstellation_publishesCreatedEvent() {
        when(constellationRepository.findByConstellationNameWithSatellites(CONSTELLATION_NAME))
                .thenReturn(Optional.of(constellation));
        when(constellationRepository.save(any(SatelliteConstellation.class))).thenReturn(constellation);

        constellationService.addSatelliteToConstellation(CONSTELLATION_NAME, satellite);

        verify(satelliteEventPublisher, times(1))
                .publishSatelliteCreated(satellite, CONSTELLATION_NAME);
    }

    @Test
    @DisplayName("[Kafka] deleteConstellation() публикует SATELLITE_DELETED для каждого спутника")
    void deleteConstellation_publishesDeletedEventForEachSatellite() {
        constellation.addSatellite(satellite);
        when(constellationRepository.findByConstellationNameWithSatellites(CONSTELLATION_NAME))
                .thenReturn(Optional.of(constellation));

        constellationService.deleteConstellation(CONSTELLATION_NAME);

        verify(satelliteEventPublisher, times(1))
                .publishSatelliteDeleted(satellite.getName(), CONSTELLATION_NAME);
    }

    @Test
    @DisplayName("getAllConstellations() делегирует findAll() и возвращает список")
    void getAllConstellations_delegatesToRepositoryFindAll() {
        when(constellationRepository.findAll()).thenReturn(List.of(constellation));

        List<SatelliteConstellation> result = constellationService.getAllConstellations();

        assertTrue(result.contains(constellation));
        verify(constellationRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("showConstellationStatus() не бросает исключение для существующей группировки")
    void showConstellationStatus_existingConstellation_doesNotThrow() {
        constellation.addSatellite(satellite);
        when(constellationRepository.findByConstellationNameWithSatellites(CONSTELLATION_NAME))
                .thenReturn(Optional.of(constellation));

        assertDoesNotThrow(() -> constellationService.showConstellationStatus(CONSTELLATION_NAME));
    }
}
