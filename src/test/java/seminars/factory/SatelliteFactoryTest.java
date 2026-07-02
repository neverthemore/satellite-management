package seminars.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seminars.domain.CommunicationSatellite;
import seminars.domain.ImagingSatellite;
import seminars.domain.Satellite;
import seminars.exception.SpaceOperationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Юнит-тесты конкретных фабрик спутников (Factory Method).
 *
 * Создание спутников теперь идёт строго через SatelliteParam — у фабрик
 * больше нет методов с примитивными параметрами и нет варианта "по
 * умолчанию", поэтому тесты на дефолтные значения (которые были раньше)
 * убраны: их сценарий просто перестал существовать.
 */
@DisplayName("SatelliteFactory: юнит-тесты конкретных фабрик")
class SatelliteFactoryTest {

    private static final String SATELLITE_NAME = "Тест-Спутник-1";
    private static final double SATELLITE_BATTERY_LEVEL = 0.7;
    private static final double CUSTOM_BANDWIDTH = 1200.0;
    private static final double CUSTOM_RESOLUTION = 3.0;

    private CommunicationSatelliteFactory communicationSatelliteFactory;
    private ImagingSatelliteFactory imagingSatelliteFactory;

    @BeforeEach
    void setUp() {
        communicationSatelliteFactory = new CommunicationSatelliteFactory();
        imagingSatelliteFactory = new ImagingSatelliteFactory();
    }

    @Test
    @DisplayName("CommunicationSatelliteFactory создаёт спутник связи с параметрами из CommunicationSatelliteParam")
    void createSatelliteWithParameter_communicationFactory_createsCommunicationSatellite() {
        CommunicationSatelliteParam param =
                new CommunicationSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, CUSTOM_BANDWIDTH);

        Satellite satellite = communicationSatelliteFactory.createSatelliteWithParameter(param);

        CommunicationSatellite communicationSatellite = assertInstanceOf(CommunicationSatellite.class, satellite);
        assertEquals(SATELLITE_NAME, communicationSatellite.getName());
        assertEquals(CUSTOM_BANDWIDTH, communicationSatellite.getBandWidth());
        assertEquals(SATELLITE_BATTERY_LEVEL, communicationSatellite.getBatteryLevel());
    }

    @Test
    @DisplayName("CommunicationSatelliteFactory выбрасывает SpaceOperationException на параметре чужого типа")
    void createSatelliteWithParameter_communicationFactory_wrongParamType_throwsException() {
        ImagingSatelliteParam wrongParam =
                new ImagingSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, CUSTOM_RESOLUTION);

        assertThrows(SpaceOperationException.class,
                () -> communicationSatelliteFactory.createSatelliteWithParameter(wrongParam));
    }

    @Test
    @DisplayName("CommunicationSatelliteFactory.isSatelliteTypeSupported() поддерживает только COMMUNICATION")
    void isSatelliteTypeSupported_communicationFactory_onlySupportsCommunication() {
        assertTrue(communicationSatelliteFactory.isSatelliteTypeSupported(SatelliteType.COMMUNICATION));
        assertFalse(communicationSatelliteFactory.isSatelliteTypeSupported(SatelliteType.IMAGE));
    }

    @Test
    @DisplayName("ImagingSatelliteFactory создаёт спутник ДЗЗ с параметрами из ImagingSatelliteParam")
    void createSatelliteWithParameter_imagingFactory_createsImagingSatellite() {
        ImagingSatelliteParam param =
                new ImagingSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, CUSTOM_RESOLUTION);

        Satellite satellite = imagingSatelliteFactory.createSatelliteWithParameter(param);

        ImagingSatellite imagingSatellite = assertInstanceOf(ImagingSatellite.class, satellite);
        assertEquals(SATELLITE_NAME, imagingSatellite.getName());
        assertEquals(CUSTOM_RESOLUTION, imagingSatellite.getResolution());
        assertEquals(0, imagingSatellite.getPhotosTaken());
    }

    @Test
    @DisplayName("ImagingSatelliteFactory выбрасывает SpaceOperationException на параметре чужого типа")
    void createSatelliteWithParameter_imagingFactory_wrongParamType_throwsException() {
        CommunicationSatelliteParam wrongParam =
                new CommunicationSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, CUSTOM_BANDWIDTH);

        assertThrows(SpaceOperationException.class,
                () -> imagingSatelliteFactory.createSatelliteWithParameter(wrongParam));
    }

    @Test
    @DisplayName("ImagingSatelliteFactory.isSatelliteTypeSupported() поддерживает только IMAGE")
    void isSatelliteTypeSupported_imagingFactory_onlySupportsImage() {
        assertTrue(imagingSatelliteFactory.isSatelliteTypeSupported(SatelliteType.IMAGE));
        assertFalse(imagingSatelliteFactory.isSatelliteTypeSupported(SatelliteType.COMMUNICATION));
    }

    @Test
    @DisplayName("Спутник, созданный фабрикой, ещё не активен и пригоден для последующей активации")
    void createSatelliteWithParameter_anyFactory_producesInactiveButActivatableSatellite() {
        CommunicationSatelliteParam param =
                new CommunicationSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, CUSTOM_BANDWIDTH);

        Satellite satellite = communicationSatelliteFactory.createSatelliteWithParameter(param);

        assertTrue(satellite.activate());
        assertTrue(satellite.isActive());
    }
}
