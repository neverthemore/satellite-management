package telemetry.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Обработка «битых» сообщений (Dead Letter Topic).
 *
 * DeadLetterPublishingRecoverer:
 *   Когда @KafkaListener бросает исключение и все retry исчерпаны,
 *   сообщение публикуется в топик satellite-events.DLT (та же партиция).
 *   DLT можно мониторить, алертить, обрабатывать вручную — без потери данных.
 *
 * FixedBackOff(1000, 3):
 *   3 попытки с интервалом 1 секунда. После 3 неудач → DLT.
 *
 * KafkaTemplate здесь — producer для записи в DLT.
 * Spring Boot автоконфигурирует KafkaTemplate<Object, Object>, если
 * spring-kafka в classpath и spring.kafka.bootstrap-servers задан.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler satelliteEventErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                // Пишем в satellite-events.DLT, в ту же партицию, что и оригинал
                (consumerRecord, exception) -> new TopicPartition(
                        consumerRecord.topic() + ".DLT",
                        consumerRecord.partition()));

        // 3 попытки × 1 сек = максимум 3 секунды перед отправкой в DLT
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3L));
    }
}
