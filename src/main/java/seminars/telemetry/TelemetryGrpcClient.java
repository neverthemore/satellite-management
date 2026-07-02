package seminars.telemetry;

import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import telemetry.grpc.TelemetryRequest;
import telemetry.grpc.TelemetryServiceGrpc;
import telemetry.grpc.TelemetryUpdate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * gRPC-клиент: подключается к satellite-telemetry и подписывается на
 * Server Streaming поток телеметрии. При каждом TelemetryUpdate обновляет
 * данные о температуре в PostgreSQL через TelemetryUpdateService.
 *
 * @GrpcClient("telemetry-service") — имя конфигурации gRPC-клиента
 * из application.yaml (grpc.client.telemetry-service.address).
 *
 * @ConditionalOnProperty — клиент создаётся только если telemetry.client.enabled=true.
 * В тестах (application-test.yaml) выставляется false, поэтому @PostConstruct
 * не вызывается и gRPC-соединение не открывается.
 *
 * ⚠️  Эти классы (TelemetryServiceGrpc, TelemetryRequest, TelemetryUpdate)
 * генерируются из telemetry.proto командой:
 *     ./gradlew generateProto
 * До её выполнения проект не скомпилируется.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "telemetry.client.enabled", havingValue = "true", matchIfMissing = false)
public class TelemetryGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(TelemetryGrpcClient.class);

    /**
     * Async stub — единственный вариант для Server Streaming.
     * Blocking stub не поддерживает бесконечные стримы.
     */
    @GrpcClient("telemetry-service")
    private TelemetryServiceGrpc.TelemetryServiceStub telemetryStub;

    private final TelemetryUpdateService telemetryUpdateService;

    private final ScheduledExecutorService retryExecutor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "telemetry-retry");
                t.setDaemon(true);
                return t;
            });

    @PostConstruct
    public void startTelemetryStream() {
        // Небольшая задержка при старте: даём время подняться другим бинам
        retryExecutor.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    private void connect() {
        log.info("▶ Подключение к gRPC телеметрическому стриму...");

        // Пустой запрос → сервис пришлёт телеметрию по всем дефолтным спутникам
        TelemetryRequest request = TelemetryRequest.newBuilder().build();

        telemetryStub.streamTelemetry(request, new StreamObserver<>() {

            @Override
            public void onNext(TelemetryUpdate update) {
                try {
                    telemetryUpdateService.applyTelemetryUpdate(update);
                } catch (Exception e) {
                    log.error("Ошибка сохранения телеметрии для {}: {}",
                            update.getSatelliteName(), e.getMessage());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.warn("✘ Ошибка телеметрического стрима: {}. Повтор через 30 сек.", t.getMessage());
                // Retry при ошибке соединения — сервис мог перезапуститься
                retryExecutor.schedule(TelemetryGrpcClient.this::connect, 30, TimeUnit.SECONDS);
            }

            @Override
            public void onCompleted() {
                log.info("◀ Телеметрический стрим завершён (сервер закрыл соединение). Повтор через 10 сек.");
                retryExecutor.schedule(TelemetryGrpcClient.this::connect, 10, TimeUnit.SECONDS);
            }
        });
    }
}
