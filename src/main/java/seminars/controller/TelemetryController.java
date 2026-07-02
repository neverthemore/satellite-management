package seminars.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seminars.domain.Satellite;
import seminars.repository.SatelliteRepository;

import java.util.List;

/**
 * REST-контроллер для просмотра данных телеметрии.
 * Возвращает температурные данные, записанные в БД через gRPC-стрим.
 */
@RestController
@RequestMapping("/api/telemetry")
@RequiredArgsConstructor
@Tag(name = "Telemetry", description = "Данные телеметрии спутников (из gRPC-стрима)")
public class TelemetryController {

    private final SatelliteRepository satelliteRepository;

    @GetMapping
    @Operation(summary = "Последние данные телеметрии для всех спутников")
    public ResponseEntity<List<TelemetryView>> getAllTelemetry() {
        List<TelemetryView> views = satelliteRepository.findAll()
                .stream()
                .map(TelemetryView::from)
                .toList();
        return ResponseEntity.ok(views);
    }

    @GetMapping("/{satelliteId}")
    @Operation(summary = "Данные телеметрии конкретного спутника")
    public ResponseEntity<TelemetryView> getTelemetry(@PathVariable Long satelliteId) {
        return satelliteRepository.findById(satelliteId)
                .map(TelemetryView::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** DTO для ответа: только поля, относящиеся к телеметрии. */
    @Getter
    @Builder
    public static class TelemetryView {
        private final Long id;
        private final String name;
        private final boolean active;
        private final Double internalTemperature;
        private final Double externalTemperature;
        private final String temperatureStatus;

        public static TelemetryView from(Satellite satellite) {
            return TelemetryView.builder()
                    .id(satellite.getId())
                    .name(satellite.getName())
                    .active(satellite.isActive())
                    .internalTemperature(satellite.getInternalTemperature())
                    .externalTemperature(satellite.getExternalTemperature())
                    .temperatureStatus(evaluateStatus(satellite.getInternalTemperature()))
                    .build();
        }

        private static String evaluateStatus(Double internalTemp) {
            if (internalTemp == null) return "NO_DATA";
            if (internalTemp < 15.0)  return "TOO_COLD";
            if (internalTemp > 40.0)  return "OVERHEATING";
            return "NOMINAL";
        }
    }
}
