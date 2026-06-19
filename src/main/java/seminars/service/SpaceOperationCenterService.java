package seminars.service;

import org.springframework.stereotype.Service;
import seminars.domain.Satellite;
import seminars.domain.SatelliteConstellation;
import seminars.repository.ConstellationRepository;

import java.util.Map;

/**
 * Сервис управления спутниковыми группировками.
 *
 * Реализация Dependency Inversion Principle: вместо того чтобы Main напрямую
 * работал с хранилищем и бизнес-логикой, всё взаимодействие проходит через
 * этот сервис. Сам сервис получает ConstellationRepository через конструктор —
 * зависимость не создаётся внутри класса (new ConstellationRepository()),
 * а внедряется снаружи Spring-контейнером (constructor injection).
 */
@Service
public class SpaceOperationCenterService {

    private final ConstellationRepository constellationRepository;

    public SpaceOperationCenterService(ConstellationRepository constellationRepository) {
        this.constellationRepository = constellationRepository;
    }

    public SatelliteConstellation createAndSaveConstellation(String name) {
        SatelliteConstellation constellation = new SatelliteConstellation(name);
        return constellationRepository.save(constellation);
    }

    public void addSatelliteToConstellation(String constellationName, Satellite satellite) {
        SatelliteConstellation constellation = getConstellationOrThrow(constellationName);
        constellation.addSatellite(satellite);
        System.out.println("Добавлен спутник " + satellite.getName() + " в группировку " + constellationName);
    }

    public void activateAllSatellites(String constellationName) {
        SatelliteConstellation constellation = getConstellationOrThrow(constellationName);
        System.out.println();
        System.out.println("=== АКТИВАЦИЯ СПУТНИКОВ В ГРУППИРОВКЕ: " + constellationName + " ===");
        for (Satellite satellite : constellation.getSatellites()) {
            if (satellite.activate()) {
                System.out.println("✅ " + satellite.getName() + ": Активация успешна");
            } else {
                System.out.println("🛑 " + satellite.getName() + ": Ошибка активации (заряд: "
                        + (int) (satellite.getBatteryLevel() * 100) + "%)");
            }
        }
    }

    public void executeConstellationMission(String constellationName) {
        SatelliteConstellation constellation = getConstellationOrThrow(constellationName);
        System.out.println();
        System.out.println("=== ВЫПОЛНЕНИЕ МИССИЙ ДЛЯ ГРУППИРОВКИ: " + constellationName + " ===");
        constellation.executeAllMissions();
    }

    public void showConstellationStatus(String constellationName) {
        SatelliteConstellation constellation = getConstellationOrThrow(constellationName);
        System.out.println();
        System.out.println("=== СТАТУС ГРУППИРОВКИ: " + constellationName + " ===");
        System.out.println("Количество спутников: " + constellation.getSatellites().size());
        for (Satellite satellite : constellation.getSatellites()) {
            System.out.println(satellite.getStatusInfo());
        }
    }

    public Map<String, SatelliteConstellation> getAllConstellations() {
        return constellationRepository.findAll();
    }

    private SatelliteConstellation getConstellationOrThrow(String name) {
        return constellationRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Группировка не найдена: " + name));
    }
}
