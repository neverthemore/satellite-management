package telemetry.service;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import telemetry.grpc.TelemetryRequest;
import telemetry.grpc.TelemetryServiceGrpc;
import telemetry.grpc.TelemetryUpdate;
import telemetry.kafka.SatelliteRegistryService;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * gRPC Server Streaming — телеметрия спутников каждые 2 секунды.
 *
 * Список спутников теперь ДИНАМИЧЕСКИЙ: берётся из SatelliteRegistryService,
 * который обновляется через Kafka при создании/удалении спутников в server.
 * Если Kafka ещё не прислала ни одного события — используется дефолтный список.
 */
@GrpcService
@RequiredArgsConstructor
public class TelemetryGrpcService extends TelemetryServiceGrpc.TelemetryServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(TelemetryGrpcService.class);
    private static final Random RANDOM = new Random();

    private final SatelliteRegistryService registryService;
    private final AtomicInteger activeStreams = new AtomicInteger(0);

    @Override
    public void streamTelemetry(TelemetryRequest request,
                                StreamObserver<TelemetryUpdate> responseObserver) {

        ServerCallStreamObserver<TelemetryUpdate> serverObserver =
                (ServerCallStreamObserver<TelemetryUpdate>) responseObserver;

        int streamId = activeStreams.incrementAndGet();
        log.info("▶ Новый телеметрический стрим #{}", streamId);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "telemetry-stream-" + streamId);
            t.setDaemon(true);
            return t;
        });

        serverObserver.setOnCancelHandler(() -> {
            log.info("◀ Клиент отключился, стрим #{} остановлен", streamId);
            executor.shutdown();
            activeStreams.decrementAndGet();
        });

        executor.scheduleAtFixedRate(() -> {
            if (serverObserver.isCancelled()) {
                executor.shutdown();
                return;
            }
            try {
                // Используем актуальный список из Kafka-реестра (или дефолт)
                List<String> satellites = request.getSatelliteNamesCount() > 0
                        ? request.getSatelliteNamesList()
                        : registryService.getActiveSatellites();

                for (String satName : satellites) {
                    serverObserver.onNext(buildUpdate(satName));
                }
            } catch (Exception e) {
                log.error("Ошибка в стриме #{}: {}", streamId, e.getMessage());
                executor.shutdown();
                activeStreams.decrementAndGet();
                serverObserver.onError(e);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private TelemetryUpdate buildUpdate(String satelliteName) {
        return TelemetryUpdate.newBuilder()
                .setSatelliteName(satelliteName)
                .setInternalTemperature(15.0 + RANDOM.nextDouble() * 20.0)
                .setExternalTemperature(-150.0 + RANDOM.nextDouble() * 270.0)
                .setBatteryLevel(0.3 + RANDOM.nextDouble() * 0.7)
                .setIsActive(RANDOM.nextDouble() > 0.1)
                .setTimestampMillis(System.currentTimeMillis())
                .build();
    }
}
