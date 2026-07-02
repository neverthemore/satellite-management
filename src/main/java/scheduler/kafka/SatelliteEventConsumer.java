package scheduler.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Консьюмер событий о спутниках в планировщике.
 *
 * Поддерживает актуальный список спутников в памяти.
 * При появлении нового спутника — регистрируем (в будущем можно
 * автоматически добавлять новую cron-задачу для него).
 * При удалении — убираем из реестра.
 */
@Service
public class SatelliteEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SatelliteEventConsumer.class);

    private final Set<String> knownSatellites = new CopyOnWriteArraySet<>();

    @KafkaListener(topics = "satellite-events", groupId = "mission-service")
    public void handleSatelliteEvent(
            @Payload SatelliteEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[Kafka][mission-service] partition={} offset={} → {} для '{}'",
                partition, offset, event.eventType(), event.satelliteName());

        switch (event.eventType()) {
            case "SATELLITE_CREATED" -> {
                knownSatellites.add(event.satelliteName());
                log.info("➕ Новый спутник '{}' в группировке '{}'. Известно спутников: {}",
                        event.satelliteName(), event.constellationName(), knownSatellites.size());
                // TODO (доп. задание): динамически добавить cron-задачу для этого спутника
                // schedulerService.addMissionForSatellite(event.satelliteName(), event.constellationName());
            }
            case "SATELLITE_DELETED" -> {
                knownSatellites.remove(event.satelliteName());
                log.info("➖ Спутник '{}' удалён. Известно спутников: {}",
                        event.satelliteName(), knownSatellites.size());
                // TODO (доп. задание): отменить запланированные миссии для этого спутника
            }
            default -> log.warn("[Kafka][mission-service] Неизвестный тип события: {}", event.eventType());
        }
    }

    /** Актуальный список спутников, известных планировщику. */
    public List<String> getKnownSatellites() {
        return List.copyOf(knownSatellites);
    }
}
