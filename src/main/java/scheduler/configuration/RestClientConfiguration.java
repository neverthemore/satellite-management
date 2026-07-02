package scheduler.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import scheduler.properties.SpaceCenterProperties;

/**
 * Конфигурация HTTP-клиента.
 *
 * RestClient — новый синхронный HTTP-клиент, появившийся в Spring Boot 3.2+,
 * современная замена RestTemplate. Здесь создаётся один бин с уже прописанным
 * базовым URL из properties, чтобы все методы SpaceOperationClient писали
 * только путь (/missions, /add-satellites и т.д.), не повторяя хост.
 */
@Configuration
public class RestClientConfiguration {

    @Bean
    public RestClient spaceOperationRestClient(SpaceCenterProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.url())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
