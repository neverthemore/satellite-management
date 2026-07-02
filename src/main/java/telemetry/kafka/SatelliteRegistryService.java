package telemetry.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Хранит актуальный список спутников, о которых узнал через Kafka.
 *
 * CopyOnWriteArraySet — потокобезопасен без явных блокировок:
 * запись (Kafka-поток) и чтение (gRPC-поток) идут конкурентно.
 *
 * До первого события из Kafka используются дефолтные спутники,
 * чтобы сервис начал стримить телеметрию сразу при старте.
 */
@Service
public class SatelliteRegistryService {

    private static final Logger log = LoggerFactory.getLogger(SatelliteRegistryService.class);

    /** Дефолтный набор — работаем «вхолостую», пока Kafka не сообщит реальных спутников. */
    private static final List<String> DEFAULT_SATELLITES =
            List.of("Связь-1", "Связь-2", "ДЗЗ-1", "ДЗЗ-2", "ДЗЗ-3");

    private final Set<String> knownSatellites = new CopyOnWriteArraySet<>();

    public void register(String satelliteName) {
        knownSatellites.add(satelliteName);
        log.info("📡 Зарегистрирован спутник: '{}' (всего: {})", satelliteName, knownSatellites.size());
    }

    public void deregister(String satelliteName) {
        knownSatellites.remove(satelliteName);
        log.info("📴 Удалён спутник: '{}' (всего: {})", satelliteName, knownSatellites.size());
    }

    /** Возвращает актуальный список. Если Kafka ещё не прислала ничего — дефолт. */
    public List<String> getActiveSatellites() {
        return knownSatellites.isEmpty() ? DEFAULT_SATELLITES : List.copyOf(knownSatellites);
    }
}
