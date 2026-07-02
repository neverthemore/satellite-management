package seminars.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seminars.domain.Satellite;
import seminars.repository.SatelliteRepository;

import java.util.List;

/**
 * CRUD-контроллер для спутников.
 * Позволяет искать и читать спутники без загрузки всей группировки.
 */
@RestController
@RequestMapping("/api/satellites")
@RequiredArgsConstructor
@Tag(name = "Satellites CRUD", description = "Прямое управление спутниками")
public class SatelliteController {

    private final SatelliteRepository satelliteRepository;

    @GetMapping
    @Operation(summary = "Получить все спутники")
    public ResponseEntity<List<Satellite>> getAll() {
        return ResponseEntity.ok(satelliteRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить спутник по ID")
    public ResponseEntity<Satellite> getById(@PathVariable Long id) {
        return satelliteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-name")
    @Operation(summary = "Найти спутник по имени")
    public ResponseEntity<Satellite> getByName(@RequestParam String name) {
        return satelliteRepository.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    @Operation(summary = "Получить все активные спутники")
    public ResponseEntity<List<Satellite>> getActive() {
        return ResponseEntity.ok(satelliteRepository.findByStateActiveTrue());
    }

    @GetMapping("/by-constellation")
    @Operation(summary = "Получить спутники конкретной группировки")
    public ResponseEntity<List<Satellite>> getByConstellation(@RequestParam String constellationName) {
        return ResponseEntity.ok(
                satelliteRepository.findByConstellationConstellationName(constellationName));
    }
}
