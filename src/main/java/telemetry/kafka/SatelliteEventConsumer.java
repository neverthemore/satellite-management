package telemetry.kafka;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

/**
 * Консьюмер событий о спутниках.
 *
 * @KafkaListener автоматически:
 *   — создаёт consumer-группу "telemetry-service"
 *   — подключается к брокеру из spring.kafka.bootstrap-servers
 *   — десериализует JSON → SatelliteEvent (через JsonDeserializer + default type)
 *   — передаёт сообщение в handleSatelliteEvent() в отдельном потоке
 *
 * Ошибки (исключения из метода) обрабатываются DefaultErrorHandler:
 *   — 3 попытки с задержкой 1 сек (FixedBackOff)
 *   — после 3 провалов → сообщение уходит в satellite-events.DLT
 */
@Service
@RequiredArgsConstructor
public class SatelliteEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SatelliteEventConsumer.class);

    private final SatelliteRegistryService registryService;

    @KafkaListener(topics = "satellite-events", groupId = "telemetry-service")
    public void handleSatelliteEvent(
            @Payload SatelliteEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[Kafka] topic=satellite-events partition={} offset={} → {} для '{}'",
                partition, offset, event.eventType(), event.satelliteName());

        switch (event.eventType()) {
            case "SATELLITE_CREATED" -> registryService.register(event.satelliteName());
            case "SATELLITE_DELETED" -> registryService.deregister(event.satelliteName());
            default -> log.warn("[Kafka] Неизвестный тип события: {}", event.eventType());
        }
    }
}
