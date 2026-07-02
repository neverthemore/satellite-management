package seminars.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seminars.domain.SatelliteConstellation;
import seminars.exception.SpaceOperationException;
import seminars.repository.SatelliteConstellationRepository;

import java.util.List;

/**
 * CRUD-контроллер для группировок.
 * Работает напрямую с JPA-репозиторием — для простых операций
 * нет нужды добавлять промежуточный сервисный слой.
 */
@RestController
@RequestMapping("/api/constellations")
@RequiredArgsConstructor
@Tag(name = "Constellations CRUD", description = "Прямое управление группировками")
public class ConstellationController {

    private final SatelliteConstellationRepository constellationRepository;

    @GetMapping
    @Operation(summary = "Получить все группировки")
    public ResponseEntity<List<SatelliteConstellation>> getAll() {
        return ResponseEntity.ok(constellationRepository.findAll());
    }

    @GetMapping("/{name}")
    @Operation(summary = "Получить группировку по имени (со спутниками)")
    public ResponseEntity<SatelliteConstellation> getByName(@PathVariable String name) {
        return constellationRepository.findByConstellationNameWithSatellites(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Создать новую группировку")
    public ResponseEntity<SatelliteConstellation> create(@RequestParam String name) {
        if (constellationRepository.existsByConstellationName(name)) {
            throw new SpaceOperationException("Группировка с именем '" + name + "' уже существует");
        }
        SatelliteConstellation created = constellationRepository.save(new SatelliteConstellation(name));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{name}")
    @Operation(summary = "Удалить группировку (каскадно удаляет спутники)")
    public ResponseEntity<Void> delete(@PathVariable String name) {
        if (!constellationRepository.existsByConstellationName(name)) {
            return ResponseEntity.notFound().build();
        }
        constellationRepository.deleteByConstellationName(name);
        return ResponseEntity.noContent().build();
    }
}
