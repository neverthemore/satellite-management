package seminars.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seminars.aop.LogExecutionTime;
import seminars.domain.Satellite;
import seminars.domain.SatelliteConstellation;
import seminars.kafka.SatelliteEventPublisher;
import seminars.repository.SatelliteConstellationRepository;
import seminars.repository.SatelliteRepository;

import java.util.List;
import java.util.Optional;

/**
 * Сервис управления группировками.
 * Публикует события в Kafka при создании и удалении спутников (Kafka-интеграция
 * семинара 12). Публикация — после фиксации транзакции БД, чтобы событие
 * не ушло раньше, чем данные попадут в PostgreSQL.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConstellationService {

    private final SatelliteConstellationRepository constellationRepository;
    private final SatelliteRepository satelliteRepository;
    private final SatelliteEventPublisher satelliteEventPublisher;

    @Transactional
    public SatelliteConstellation createAndSaveConstellation(String name) {
        SatelliteConstellation constellation = new SatelliteConstellation(name);
        return constellationRepository.save(constellation);
    }

    public boolean existsConstellation(String name) {
        return constellationRepository.existsByConstellationName(name);
    }

    public Optional<SatelliteConstellation> getConstellation(String name) {
        return constellationRepository.findByConstellationNameWithSatellites(name);
    }

    @Transactional
    public void addSatelliteToConstellation(String constellationName, Satellite satellite) {
        SatelliteConstellation constellation = getConstellationOrThrow(constellationName);
        constellation.addSatellite(satellite);
        constellationRepository.save(constellation);
        System.out.println("Добавлен спутник " + satellite.getName() + " в группировку " + constellationName);

        // Публикуем событие после успешного сохранения в БД
        satelliteEventPublisher.publishSatelliteCreated(satellite, constellationName);
    }

    @Transactional
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

    @Transactional
    public void deactivateAllSatellites(String constellationName) {
        SatelliteConstellation constellation = getConstellationOrThrow(constellationName);
        System.out.println();
        System.out.println("=== ДЕАКТИВАЦИЯ СПУТНИКОВ В ГРУППИРОВКЕ: " + constellationName + " ===");
        for (Satellite satellite : constellation.getSatellites()) {
            satellite.deactivate();
            System.out.println("🔌 " + satellite.getName() + ": деактивирован");
        }
    }

    @Transactional
    @LogExecutionTime
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

    public List<SatelliteConstellation> getAllConstellations() {
        return constellationRepository.findAll();
    }

    @Transactional
    public void deleteConstellation(String constellationName) {
        // Сначала публикуем DELETED для каждого спутника, потом удаляем каскадно
        constellationRepository.findByConstellationNameWithSatellites(constellationName)
                .ifPresent(constellation -> constellation.getSatellites()
                        .forEach(satellite -> satelliteEventPublisher
                                .publishSatelliteDeleted(satellite.getName(), constellationName)));
        constellationRepository.deleteByConstellationName(constellationName);
    }

    private SatelliteConstellation getConstellationOrThrow(String name) {
        return constellationRepository.findByConstellationNameWithSatellites(name)
                .orElseThrow(() -> new IllegalArgumentException("Группировка не найдена: " + name));
    }
}
