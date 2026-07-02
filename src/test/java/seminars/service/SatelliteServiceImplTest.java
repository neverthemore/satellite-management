package seminars.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seminars.domain.CommunicationSatellite;
import seminars.domain.Satellite;
import seminars.exception.SpaceOperationException;
import seminars.factory.CommunicationSatelliteFactory;
import seminars.factory.CommunicationSatelliteParam;
import seminars.factory.ImagingSatelliteFactory;
import seminars.factory.ImagingSatelliteParam;
import seminars.factory.SatelliteFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Юнит-тест SatelliteServiceImpl без Spring — список фабрик собирается
 * вручную, что позволяет проверить и happy path, и граничный случай
 * "ни одна фабрика не поддерживает запрошенный тип" (в реальном
 * Spring-контексте такого не получить, там обе фабрики всегда есть).
 */
@DisplayName("SatelliteServiceImpl: юнит-тесты выбора фабрики (Strategy)")
class SatelliteServiceImplTest {

    private static final String SATELLITE_NAME = "Сервис-Юнит-1";
    private static final double SATELLITE_BATTERY_LEVEL = 0.65;
    private static final double SATELLITE_BANDWIDTH = 400.0;
    private static final double SATELLITE_RESOLUTION = 1.8;

    private SatelliteServiceImpl satelliteService;

    @BeforeEach
    void setUp() {
        List<SatelliteFactory> factories = List.of(new CommunicationSatelliteFactory(), new ImagingSatelliteFactory());
        satelliteService = new SatelliteServiceImpl(factories);
    }

    @Test
    @DisplayName("createSatellite() выбирает CommunicationSatelliteFactory для CommunicationSatelliteParam")
    void createSatellite_communicationParam_returnsCommunicationSatellite() {
        Satellite satellite = satelliteService.createSatellite(
                new CommunicationSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, SATELLITE_BANDWIDTH));

        CommunicationSatellite communicationSatellite = assertInstanceOf(CommunicationSatellite.class, satellite);
        assertEquals(SATELLITE_BANDWIDTH, communicationSatellite.getBandWidth());
    }

    @Test
    @DisplayName("createSatellite() выбрасывает SpaceOperationException, если ни одна фабрика не поддерживает тип параметра")
    void createSatellite_noMatchingFactoryRegistered_throwsSpaceOperationException() {
        SatelliteServiceImpl serviceWithOnlyCommunicationFactory =
                new SatelliteServiceImpl(List.of(new CommunicationSatelliteFactory()));
        ImagingSatelliteParam param = new ImagingSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, SATELLITE_RESOLUTION);

        assertThrows(SpaceOperationException.class, () -> serviceWithOnlyCommunicationFactory.createSatellite(param));
    }

    @Test
    @DisplayName("createSatellite() выбрасывает SpaceOperationException, если список фабрик пуст")
    void createSatellite_emptyFactoryList_throwsSpaceOperationException() {
        SatelliteServiceImpl serviceWithoutFactories = new SatelliteServiceImpl(List.of());
        CommunicationSatelliteParam param =
                new CommunicationSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, SATELLITE_BANDWIDTH);

        assertThrows(SpaceOperationException.class, () -> serviceWithoutFactories.createSatellite(param));
    }
}
