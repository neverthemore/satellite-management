package seminars.telemetry;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seminars.repository.SatelliteRepository;
import telemetry.grpc.TelemetryUpdate;

/**
 * Сервис сохранения телеметрии в базу данных.
 *
 * Вынесен в отдельный @Service, чтобы @Transactional работал корректно:
 * gRPC callback (onNext) выполняется в gRPC-потоке, который НЕ является
 * Spring-управляемым. Вызов @Transactional-метода через Spring-прокси
 * из другого @Service — правильный способ работы с транзакциями в такой ситуации.
 *
 * Если бы @Transactional стоял прямо на методе внутри TelemetryGrpcClient,
 * он бы не работал (Spring AOP требует вызова через прокси-объект).
 */
@Service
@RequiredArgsConstructor
public class TelemetryUpdateService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryUpdateService.class);

    private final SatelliteRepository satelliteRepository;

    /**
     * Применяет одно обновление телеметрии из gRPC-стрима:
     * находит спутник по имени и обновляет его температурные поля.
     *
     * Если спутник с таким именем ещё не существует в БД — обновление
     * пропускается (телеметрия пришла раньше, чем добавили спутник).
     */
    @Transactional
    public void applyTelemetryUpdate(TelemetryUpdate update) {
        satelliteRepository.findByName(update.getSatelliteName())
                .ifPresentOrElse(
                        satellite -> {
                            satellite.updateTelemetry(
                                    update.getInternalTemperature(),
                                    update.getExternalTemperature());
                            // dirty checking: @Transactional → Hibernate сохранит изменения
                            log.debug("🌡 Телеметрия обновлена: {} → внутр={:.1f}°C, внешн={:.1f}°C",
                                    update.getSatelliteName(),
                                    update.getInternalTemperature(),
                                    update.getExternalTemperature());
                        },
                        () -> log.debug("⚠ Спутник '{}' не найден в БД — телеметрия пропущена",
                                update.getSatelliteName())
                );
    }
}
