package seminars.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Создаёт топики в Kafka при старте приложения (через AdminClient).
 * Если топик уже существует — ничего не делает (idempotent).
 *
 * satellite-events    — основной топик событий (3 партиции: масштабирование)
 * satellite-events.DLT — Dead Letter Topic для «битых» сообщений
 *   (консьюмер не смог обработать после N попыток → сообщение уходит сюда
 *   для ручного разбора или мониторинга, а не теряется).
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic satelliteEventsTopic() {
        return TopicBuilder.name(SatelliteEventPublisher.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic satelliteEventsDlt() {
        return TopicBuilder.name(SatelliteEventPublisher.TOPIC + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
