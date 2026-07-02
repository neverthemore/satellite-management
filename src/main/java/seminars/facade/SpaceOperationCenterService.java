package seminars.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seminars.aop.LogExecutionTime;
import seminars.domain.Satellite;
import seminars.domain.SatelliteConstellation;
import seminars.exception.SpaceOperationException;
import seminars.factory.SatelliteParam;
import seminars.service.ConstellationService;
import seminars.service.SatelliteService;

import java.util.ArrayList;
import java.util.List;

/**
 * Фасад (Facade, GoF) над бизнес-логикой системы управления спутниковой
 * группировкой.
 *
 * До рефакторинга вызывающему коду (см. старую версию Main) приходилось
 * самому знать, в каком порядке дёргать ConstellationService и SatelliteService,
 * чтобы получить осмысленный результат: проверить, существует ли группировка,
 * создать каждый спутник через SatelliteService, добавить каждый через
 * ConstellationService, отдельно активировать, отдельно выполнить миссии.
 * Фасад прячет всю эту последовательность за несколькими простыми методами.
 *
 * Сам фасад не содержит бизнес-логики — он только ОРКЕСТРИРУЕТ вызовы
 * нижележащих сервисов в правильном порядке.
 */
@Service
@RequiredArgsConstructor
public class SpaceOperationCenterService {

    private final ConstellationService constellationService;
    private final SatelliteService satelliteService;

    /**
     * Добавляет один или несколько спутников в группировку, создавая её,
     * если она ещё не существует.
     *
     * Раньше это были 3 разных шага через 2 разных сервиса для КАЖДОГО
     * спутника — теперь один вызов на всю партию.
     */
    @LogExecutionTime
    public List<Satellite> addSatellite(AddSatelliteRequest request) {
        if (!constellationService.existsConstellation(request.getConstellationName())) {
            constellationService.createAndSaveConstellation(request.getConstellationName());
        }

        List<Satellite> addedSatellites = new ArrayList<>();
        for (SatelliteParam param : request.getSatelliteParams()) {
            Satellite satellite = satelliteService.createSatellite(param);
            constellationService.addSatelliteToConstellation(request.getConstellationName(), satellite);
            addedSatellites.add(satellite);
        }
        return addedSatellites;
    }

    /**
     * Выполняет миссии указанной группировки, при необходимости предварительно
     * активировав все её спутники.
     */
    @LogExecutionTime
    public void executeMission(MissionRequest request) {
        if (request.isActivateBeforeMission()) {
            constellationService.activateAllSatellites(request.getConstellationName());
        }
        constellationService.executeConstellationMission(request.getConstellationName());
    }

    /**
     * Полный цикл развёртывания группировки одним вызовом: создание группировки
     * и спутников -> активация -> выполнение миссий. То, что раньше требовало
     * 3-4 отдельных вызова через два сервиса, теперь — один метод.
     */
    public List<Satellite> deployConstellation(AddSatelliteRequest request) {
        List<Satellite> satellites = addSatellite(request);
        executeMission(MissionRequest.builder()
                .constellationName(request.getConstellationName())
                .activateBeforeMission(true)
                .build());
        return satellites;
    }

    /** Краткая сводка по группировке: сколько всего спутников и сколько из них активны. */
    public ConstellationStatusReport getConstellationReport(String constellationName) {
        SatelliteConstellation constellation = constellationService.getConstellation(constellationName)
                .orElseThrow(() -> new SpaceOperationException("Группировка не найдена: " + constellationName));

        long activeCount = constellation.getSatellites().stream()
                .filter(Satellite::isActive)
                .count();

        return ConstellationStatusReport.builder()
                .constellationName(constellationName)
                .totalSatellites(constellation.getSatellites().size())
                .activeSatellites((int) activeCount)
                .build();
    }
}
