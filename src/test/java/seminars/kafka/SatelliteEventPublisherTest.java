package seminars.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import seminars.domain.CommunicationSatellite;
import seminars.domain.Satellite;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Тест Kafka-продюсера с встроенным брокером (@EmbeddedKafka).
 *
 * @EmbeddedKafka поднимает настоящий Kafka-брокер в памяти JVM —
 * никакого внешнего Docker-Kafka не нужно. Идеально для CI/CD.
 *
 * @TestPropertySource переопределяет bootstrap-servers так, чтобы
 * KafkaTemplate подключился к встроенному брокеру, а не к localhost:9094.
 *
 * Мы сами создаём consumer (ContainerProperties + KafkaMessageListenerContainer),
 * чтобы не зависеть от @KafkaListener — это изолированный тест только продюсера.
 */
@SpringBootTest(classes = seminars.Main.class)
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = { SatelliteEventPublisher.TOPIC, SatelliteEventPublisher.TOPIC + ".DLT" }
)
@TestPropertySource(properties = {
        // Направляем KafkaTemplate на встроенный брокер
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.autoconfigure.exclude=",   // включаем Kafka (была выключена в test-профиле)
        "telemetry.client.enabled=false"
})
@DisplayName("SatelliteEventPublisher: тест с EmbeddedKafka")
class SatelliteEventPublisherTest {

    @Autowired
    private SatelliteEventPublisher publisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private KafkaMessageListenerContainer<String, SatelliteEvent> container;
    private BlockingQueue<ConsumerRecord<String, SatelliteEvent>> receivedRecords;

    @BeforeEach
    void setUp() {
        receivedRecords = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group", "true", embeddedKafka);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SatelliteEvent.class.getName());

        DefaultKafkaConsumerFactory<String, SatelliteEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties =
                new ContainerProperties(SatelliteEventPublisher.TOPIC);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, SatelliteEvent>)
                record -> receivedRecords.add(record));
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        container.stop();
    }

    @Test
    @DisplayName("publishSatelliteCreated() отправляет SATELLITE_CREATED событие в топик")
    void publishSatelliteCreated_messageLandsInTopic() throws InterruptedException {
        Satellite satellite = new CommunicationSatellite("Связь-Kafka-Тест", 0.85, 500.0);

        publisher.publishSatelliteCreated(satellite, "Орбита-Тест");

        // Ждём получения сообщения (до 5 секунд)
        ConsumerRecord<String, SatelliteEvent> record =
                receivedRecords.poll(5, TimeUnit.SECONDS);

        assertNotNull(record, "Событие не пришло в топик за 5 секунд");
        assertEquals("Связь-Kafka-Тест", record.key());
        assertEquals("SATELLITE_CREATED", record.value().eventType());
        assertEquals("Орбита-Тест", record.value().constellationName());
        assertEquals("COMMUNICATION", record.value().satelliteType());
    }

    @Test
    @DisplayName("publishSatelliteDeleted() отправляет SATELLITE_DELETED событие в топик")
    void publishSatelliteDeleted_messageLandsInTopic() throws InterruptedException {
        publisher.publishSatelliteDeleted("ДЗЗ-Kafka-Тест", "Орбита-Тест");

        ConsumerRecord<String, SatelliteEvent> record =
                receivedRecords.poll(5, TimeUnit.SECONDS);

        assertNotNull(record, "Событие не пришло в топик за 5 секунд");
        assertEquals("SATELLITE_DELETED", record.value().eventType());
        assertEquals("ДЗЗ-Kafka-Тест", record.value().satelliteName());
    }
}
