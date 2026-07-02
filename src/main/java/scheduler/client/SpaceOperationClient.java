package scheduler.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import scheduler.missions.MissionRequest;

/**
 * Клиент для взаимодействия с основным сервисом управления спутниками.
 *
 * Инкапсулирует все HTTP-вызовы через RestClient — если завтра нужно
 * сменить транспорт на Feign или другой клиент, меняется только этот класс,
 * а планировщик остаётся нетронутым (SRP + DIP).
 */
@Service
public class SpaceOperationClient {

    private static final Logger log = LoggerFactory.getLogger(SpaceOperationClient.class);

    private final RestClient restClient;

    public SpaceOperationClient(RestClient spaceOperationRestClient) {
        this.restClient = spaceOperationRestClient;
    }

    /**
     * Отправляет запрос на выполнение миссии группировки.
     *
     * @return true, если основной сервис вернул успешный ответ
     */
    public boolean executeMission(MissionRequest request) {
        try {
            restClient.post()
                    .uri("/missions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Миссия для '{}' успешно выполнена", request.constellationName());
            return true;
        } catch (RestClientException e) {
            log.error("Ошибка при выполнении миссии для '{}': {}", request.constellationName(), e.getMessage());
            return false;
        }
    }
}
