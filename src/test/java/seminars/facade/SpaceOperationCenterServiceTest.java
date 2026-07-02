package seminars.facade;

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
import seminars.exception.SpaceOperationException;
import seminars.factory.CommunicationSatelliteParam;
import seminars.factory.ImagingSatelliteParam;
import seminars.service.ConstellationService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционные тесты фасада SpaceOperationCenterService.
 *
 * Каждый тест проверяет, что несколько шагов через ConstellationService и
 * SatelliteService, спрятанные за одним методом фасада, действительно
 * выполняются и приводят к ожидаемому состоянию — то есть оркестрация
 * фасада корректна, а не просто компилируется.
 */
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@SpringBootTest(classes = Main.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("SpaceOperationCenterService (Facade): интеграционные тесты")
class SpaceOperationCenterServiceTest {

    private static final String CONSTELLATION_NAME = "Фасад-Группировка";

    private static final String COMMUNICATION_SATELLITE_NAME = "Связь-Фасад-1";
    private static final double COMMUNICATION_SATELLITE_BATTERY = 0.8;
    private static final double COMMUNICATION_SATELLITE_BANDWIDTH = 600.0;

    private static final String IMAGING_SATELLITE_NAME = "ДЗЗ-Фасад-1";
    private static final double IMAGING_SATELLITE_BATTERY = 0.9;
    private static final double IMAGING_SATELLITE_RESOLUTION = 2.2;

    @Autowired
    private SpaceOperationCenterService operationCenter;

    @Autowired
    private ConstellationService constellationService;

    @Test
    @DisplayName("addSatellite() создаёт группировку (если её не было) и добавляет в неё все переданные спутники")
    void addSatellite_newConstellation_createsItAndAddsAllSatellites() {
        String constellationName = CONSTELLATION_NAME + "-new";

        AddSatelliteRequest request = AddSatelliteRequest.builder()
                .constellationName(constellationName)
                .satelliteParam(new CommunicationSatelliteParam(
                        COMMUNICATION_SATELLITE_NAME, COMMUNICATION_SATELLITE_BATTERY, COMMUNICATION_SATELLITE_BANDWIDTH))
                .satelliteParam(new ImagingSatelliteParam(
                        IMAGING_SATELLITE_NAME, IMAGING_SATELLITE_BATTERY, IMAGING_SATELLITE_RESOLUTION))
                .build();

        List<Satellite> addedSatellites = operationCenter.addSatellite(request);

        assertEquals(2, addedSatellites.size());
        assertTrue(constellationService.existsConstellation(constellationName));
        assertEquals(2, constellationService.getConstellation(constellationName).orElseThrow().getSatellites().size());
    }

    @Test
    @DisplayName("addSatellite() для уже существующей группировки добавляет спутники, не создавая её повторно")
    void addSatellite_existingConstellation_addsWithoutRecreating() {
        String constellationName = CONSTELLATION_NAME + "-existing";

        operationCenter.addSatellite(AddSatelliteRequest.builder()
                .constellationName(constellationName)
                .satelliteParam(new CommunicationSatelliteParam(
                        COMMUNICATION_SATELLITE_NAME, COMMUNICATION_SATELLITE_BATTERY, COMMUNICATION_SATELLITE_BANDWIDTH))
                .build());

        operationCenter.addSatellite(AddSatelliteRequest.builder()
                .constellationName(constellationName)
                .satelliteParam(new ImagingSatelliteParam(
                        IMAGING_SATELLITE_NAME, IMAGING_SATELLITE_BATTERY, IMAGING_SATELLITE_RESOLUTION))
                .build());

        assertEquals(2, constellationService.getConstellation(constellationName).orElseThrow().getSatellites().size());
    }

    @Test
    @DisplayName("executeMission() по умолчанию активирует спутники перед выполнением миссий")
    void executeMission_defaultActivateBeforeMission_activatesThenExecutes() {
        String constellationName = CONSTELLATION_NAME + "-mission";

        List<Satellite> satellites = operationCenter.addSatellite(AddSatelliteRequest.builder()
                .constellationName(constellationName)
                .satelliteParam(new CommunicationSatelliteParam(
                        COMMUNICATION_SATELLITE_NAME, COMMUNICATION_SATELLITE_BATTERY, COMMUNICATION_SATELLITE_BANDWIDTH))
                .build());

        operationCenter.executeMission(MissionRequest.builder()
                .constellationName(constellationName)
                .build());

        assertTrue(satellites.get(0).isActive());
        assertTrue(satellites.get(0).getBatteryLevel() < COMMUNICATION_SATELLITE_BATTERY);
    }

    @Test
    @DisplayName("deployConstellation() одним вызовом создаёт спутники, активирует их и выполняет миссии")
    void deployConstellation_fullPipeline_createsActivatesAndExecutes() {
        String constellationName = CONSTELLATION_NAME + "-deploy";

        List<Satellite> satellites = operationCenter.deployConstellation(AddSatelliteRequest.builder()
                .constellationName(constellationName)
                .satelliteParam(new ImagingSatelliteParam(
                        IMAGING_SATELLITE_NAME, IMAGING_SATELLITE_BATTERY, IMAGING_SATELLITE_RESOLUTION))
                .build());

        ImagingSatellite imagingSatellite = assertInstanceOf(ImagingSatellite.class, satellites.get(0));
        assertTrue(imagingSatellite.isActive());
        assertEquals(1, imagingSatellite.getPhotosTaken());
    }

    @Test
    @DisplayName("getConstellationReport() возвращает корректные счётчики до и после активации")
    void getConstellationReport_returnsCorrectCountsBeforeAndAfterActivation() {
        String constellationName = CONSTELLATION_NAME + "-report";

        operationCenter.addSatellite(AddSatelliteRequest.builder()
                .constellationName(constellationName)
                .satelliteParam(new CommunicationSatelliteParam(
                        COMMUNICATION_SATELLITE_NAME, COMMUNICATION_SATELLITE_BATTERY, COMMUNICATION_SATELLITE_BANDWIDTH))
                .satelliteParam(new ImagingSatelliteParam(
                        IMAGING_SATELLITE_NAME, IMAGING_SATELLITE_BATTERY, IMAGING_SATELLITE_RESOLUTION))
                .build());

        ConstellationStatusReport reportBeforeActivation = operationCenter.getConstellationReport(constellationName);
        assertEquals(2, reportBeforeActivation.getTotalSatellites());
        assertEquals(0, reportBeforeActivation.getActiveSatellites());

        operationCenter.executeMission(MissionRequest.builder().constellationName(constellationName).build());

        ConstellationStatusReport reportAfterActivation = operationCenter.getConstellationReport(constellationName);
        assertEquals(2, reportAfterActivation.getActiveSatellites());
    }

    @Test
    @DisplayName("getConstellationReport() для несуществующей группировки выбрасывает SpaceOperationException")
    void getConstellationReport_unknownConstellation_throwsException() {
        assertThrows(SpaceOperationException.class,
                () -> operationCenter.getConstellationReport("Несуществующая-Фасад-Группировка"));
    }
}
