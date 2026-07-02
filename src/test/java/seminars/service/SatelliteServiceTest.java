package seminars.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import seminars.kafka.TestKafkaConfig;
import org.springframework.test.context.ActiveProfiles;
import seminars.Main;
import seminars.domain.CommunicationSatellite;
import seminars.domain.ImagingSatellite;
import seminars.domain.Satellite;
import seminars.factory.CommunicationSatelliteParam;
import seminars.factory.ImagingSatelliteParam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Интеграционный тест SatelliteServiceImpl на реально поднятом Spring-контексте:
 * сервис получает СВОЙ List<SatelliteFactory> через настоящую сборку бинов
 * (а не через ручной new List.of(...)), поэтому здесь же фактически
 * проверяется и то, что обе фабрики корректно зарегистрированы как @Component
 * и подхвачены Spring'ом.
 */
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@SpringBootTest(classes = Main.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("SatelliteServiceImpl: интеграционные тесты на реальном Spring-контексте")
class SatelliteServiceTest {

    private static final String IMAGING_SATELLITE_NAME = "ДЗЗ-Сервис-1";
    private static final double IMAGING_SATELLITE_BATTERY = 0.7;
    private static final double IMAGING_SATELLITE_RESOLUTION = 2.0;

    private static final String COMMUNICATION_SATELLITE_NAME = "Связь-Сервис-1";
    private static final double COMMUNICATION_SATELLITE_BATTERY = 0.6;
    private static final double COMMUNICATION_SATELLITE_BANDWIDTH = 800.0;

    @Autowired
    private SatelliteService satelliteService;

    @Test
    @DisplayName("createSatellite() с ImagingSatelliteParam возвращает ImagingSatellite с заданными характеристиками")
    void createSatellite_imagingParam_returnsConfiguredImagingSatellite() {
        ImagingSatelliteParam param = new ImagingSatelliteParam(
                IMAGING_SATELLITE_NAME, IMAGING_SATELLITE_BATTERY, IMAGING_SATELLITE_RESOLUTION);

        Satellite satellite = satelliteService.createSatellite(param);

        ImagingSatellite imagingSatellite = assertInstanceOf(ImagingSatellite.class, satellite);
        assertEquals(IMAGING_SATELLITE_NAME, imagingSatellite.getName());
        assertEquals(IMAGING_SATELLITE_RESOLUTION, imagingSatellite.getResolution());
        assertEquals(IMAGING_SATELLITE_BATTERY, imagingSatellite.getBatteryLevel());
    }

    @Test
    @DisplayName("createSatellite() с CommunicationSatelliteParam возвращает CommunicationSatellite с заданными характеристиками")
    void createSatellite_communicationParam_returnsConfiguredCommunicationSatellite() {
        CommunicationSatelliteParam param = new CommunicationSatelliteParam(
                COMMUNICATION_SATELLITE_NAME, COMMUNICATION_SATELLITE_BATTERY, COMMUNICATION_SATELLITE_BANDWIDTH);

        Satellite satellite = satelliteService.createSatellite(param);

        CommunicationSatellite communicationSatellite = assertInstanceOf(CommunicationSatellite.class, satellite);
        assertEquals(COMMUNICATION_SATELLITE_NAME, communicationSatellite.getName());
        assertEquals(COMMUNICATION_SATELLITE_BANDWIDTH, communicationSatellite.getBandWidth());
        assertEquals(COMMUNICATION_SATELLITE_BATTERY, communicationSatellite.getBatteryLevel());
    }

    @Test
    @DisplayName("Сервис выбирает правильную фабрику для каждого типа параметра независимо от порядка вызовов (Strategy)")
    void createSatellite_multipleParamTypes_selectsMatchingFactoryEachTime() {
        Satellite imagingSatellite = satelliteService.createSatellite(
                new ImagingSatelliteParam(IMAGING_SATELLITE_NAME, IMAGING_SATELLITE_BATTERY, IMAGING_SATELLITE_RESOLUTION));
        Satellite communicationSatellite = satelliteService.createSatellite(
                new CommunicationSatelliteParam(COMMUNICATION_SATELLITE_NAME, COMMUNICATION_SATELLITE_BATTERY, COMMUNICATION_SATELLITE_BANDWIDTH));

        assertInstanceOf(ImagingSatellite.class, imagingSatellite);
        assertInstanceOf(CommunicationSatellite.class, communicationSatellite);
    }
}
