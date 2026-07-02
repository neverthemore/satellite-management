package seminars.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seminars.domain.Satellite;
import seminars.domain.SatelliteConstellation;
import seminars.facade.AddSatelliteRequest;
import seminars.facade.ConstellationStatusReport;
import seminars.facade.MissionRequest;
import seminars.facade.SpaceOperationCenterService;
import seminars.service.ConstellationService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Space Operation Center", description = "API управления спутниковой группировкой")
public class SpaceOperationController {

    private final SpaceOperationCenterService spaceOperationCenterService;
    private final ConstellationService constellationService;

    @PostMapping("/add-satellites")
    @Operation(summary = "Добавить спутники в группировку")
    public ResponseEntity<List<String>> addSatellites(@RequestBody AddSatelliteRequest request) {
        List<Satellite> added = spaceOperationCenterService.addSatellite(request);
        return ResponseEntity.ok(added.stream().map(Satellite::getName).toList());
    }

    @PostMapping("/missions")
    @Operation(summary = "Выполнить миссию группировки")
    public ResponseEntity<Void> executeMission(@RequestBody MissionRequest request) {
        spaceOperationCenterService.executeMission(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deploy")
    @Operation(summary = "Развернуть группировку (создать + активировать + выполнить миссии)")
    public ResponseEntity<List<String>> deployConstellation(@RequestBody AddSatelliteRequest request) {
        List<Satellite> deployed = spaceOperationCenterService.deployConstellation(request);
        return ResponseEntity.ok(deployed.stream().map(Satellite::getName).toList());
    }

    @GetMapping("/overview")
    @Operation(summary = "Получить сводку по всем группировкам")
    public ResponseEntity<Map<String, Object>> getOverview() {
        List<SatelliteConstellation> constellations = constellationService.getAllConstellations();
        Map<String, Object> summary = constellations.stream()
                .collect(Collectors.toMap(
                        SatelliteConstellation::getConstellationName,
                        c -> Map.of(
                                "id", c.getId(),
                                "totalSatellites", c.getSatellites().size(),
                                "activeSatellites", c.getSatellites().stream().filter(Satellite::isActive).count()
                        )
                ));
        return ResponseEntity.ok(Map.of(
                "totalConstellations", constellations.size(),
                "constellations", summary
        ));
    }

    @GetMapping("/constellations/{constellationName}/report")
    @Operation(summary = "Получить сводку по конкретной группировке")
    public ResponseEntity<ConstellationStatusReport> getConstellationReport(
            @PathVariable @Parameter(description = "Имя группировки") String constellationName) {
        return ResponseEntity.ok(spaceOperationCenterService.getConstellationReport(constellationName));
    }

    @DeleteMapping("/constellations/{constellationName}/satellites/{satelliteName}")
    @Operation(summary = "Вывести спутник из эксплуатации (деактивировать)")
    public ResponseEntity<String> decommissionSatellite(
            @PathVariable String constellationName,
            @PathVariable String satelliteName) {
        return constellationService.getConstellation(constellationName)
                .flatMap(c -> c.getSatellites().stream()
                        .filter(s -> s.getName().equals(satelliteName))
                        .findFirst())
                .map(satellite -> {
                    satellite.deactivate();
                    return ResponseEntity.ok("Спутник " + satelliteName + " деактивирован");
                })
                .orElse(ResponseEntity.notFound().<String>build());
    }
}
