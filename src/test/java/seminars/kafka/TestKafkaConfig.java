package seminars.kafka;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * Тестовая конфигурация Kafka.
 *
 * В профиле "test" Kafka auto-config выключен (application-test.yaml).
 * Чтобы ConstellationService мог стартовать (он инжектирует
 * SatelliteEventPublisher), нам нужен mock-бин вместо реального.
 *
 * @Primary гарантирует, что этот mock перекроет любой другой бин
 * типа SatelliteEventPublisher в тестовом контексте.
 *
 * Использование: добавить @Import(TestKafkaConfig.class) к @SpringBootTest-тестам,
 * которые касаются ConstellationService, или подключить через @ActiveProfiles("test").
 */
@TestConfiguration
public class TestKafkaConfig {

    @Bean
    @Primary
    public SatelliteEventPublisher mockSatelliteEventPublisher() {
        return mock(SatelliteEventPublisher.class);
    }
}
