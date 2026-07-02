package seminars.telemetry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import seminars.domain.CommunicationSatellite;
import seminars.domain.Satellite;
import seminars.repository.SatelliteRepository;
import telemetry.grpc.TelemetryUpdate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Юнит-тест TelemetryUpdateService.
 * Проверяет логику применения gRPC TelemetryUpdate к сущности Satellite.
 *
 * ⚠️  TelemetryUpdate — сгенерированный из .proto класс.
 * Тест скомпилируется только после ./gradlew generateProto в satellite-spring.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryUpdateService: юнит-тесты")
class TelemetryUpdateServiceTest {

    private static final String SAT_NAME = "Связь-Тест-1";
    private static final double INTERNAL_TEMP = 22.5;
    private static final double EXTERNAL_TEMP = -87.3;

    @Mock
    private SatelliteRepository satelliteRepository;

    @InjectMocks
    private TelemetryUpdateService telemetryUpdateService;

    @Test
    @DisplayName("applyTelemetryUpdate() обновляет температуру спутника, если он есть в БД")
    void applyTelemetryUpdate_knownSatellite_updatesTemperatureFields() {
        Satellite satellite = new CommunicationSatellite(SAT_NAME, 0.8, 500.0);
        when(satelliteRepository.findByName(SAT_NAME)).thenReturn(Optional.of(satellite));

        TelemetryUpdate update = TelemetryUpdate.newBuilder()
                .setSatelliteName(SAT_NAME)
                .setInternalTemperature(INTERNAL_TEMP)
                .setExternalTemperature(EXTERNAL_TEMP)
                .build();

        telemetryUpdateService.applyTelemetryUpdate(update);

        assertEquals(INTERNAL_TEMP, satellite.getInternalTemperature());
        assertEquals(EXTERNAL_TEMP, satellite.getExternalTemperature());
        verify(satelliteRepository, times(1)).findByName(SAT_NAME);
    }

    @Test
    @DisplayName("applyTelemetryUpdate() пропускает обновление, если спутник не найден в БД")
    void applyTelemetryUpdate_unknownSatellite_silentlySkips() {
        when(satelliteRepository.findByName(SAT_NAME)).thenReturn(Optional.empty());

        TelemetryUpdate update = TelemetryUpdate.newBuilder()
                .setSatelliteName(SAT_NAME)
                .setInternalTemperature(INTERNAL_TEMP)
                .setExternalTemperature(EXTERNAL_TEMP)
                .build();

        // Не бросает исключение, просто пропускает
        telemetryUpdateService.applyTelemetryUpdate(update);
        verify(satelliteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
