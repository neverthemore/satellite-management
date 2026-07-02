package seminars.kafka;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import seminars.domain.CommunicationSatellite;
import seminars.domain.Satellite;

/**
 * Публикует события об изменении спутников в Kafka.
 *
 * Kafka — асинхронный, ненадёжный канал (Kafka может быть недоступна).
 * Поэтому:
 *   1. Публикация НЕ БЛОКИРУЕТ основной поток (KafkaTemplate.send — async).
 *   2. Ошибка публикации логируется, но НЕ прерывает транзакцию БД.
 *   3. В production: Transactional Outbox Pattern решает это правильно.
 */
@Service
@RequiredArgsConstructor
public class SatelliteEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SatelliteEventPublisher.class);
    static final String TOPIC = "satellite-events";

    private final KafkaTemplate<String, SatelliteEvent> kafkaTemplate;

    public void publishSatelliteCreated(Satellite satellite, String constellationName) {
        String type = satellite instanceof CommunicationSatellite ? "COMMUNICATION" : "IMAGE";
        send(SatelliteEvent.created(
                satellite.getName(), constellationName, type, satellite.getBatteryLevel()));
    }

    public void publishSatelliteDeleted(String satelliteName, String constellationName) {
        send(SatelliteEvent.deleted(satelliteName, constellationName));
    }

    private void send(SatelliteEvent event) {
        try {
            // Ключ = имя спутника → все события одного спутника в одну партицию
            kafkaTemplate.send(TOPIC, event.satelliteName(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("✘ Не удалось опубликовать {} для '{}': {}",
                                    event.eventType(), event.satelliteName(), ex.getMessage());
                        } else {
                            log.info("✔ Kafka: {} → спутник='{}', partition={}",
                                    event.eventType(), event.satelliteName(),
                                    result.getRecordMetadata().partition());
                        }
                    });
        } catch (Exception e) {
            // Kafka недоступна — не блокируем основной бизнес-поток
            log.error("✘ Kafka недоступна, событие {} потеряно: {}", event.eventType(), e.getMessage());
        }
    }
}
