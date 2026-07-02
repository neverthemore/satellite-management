package seminars.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import seminars.aop.LogExecutionTime;
import seminars.domain.Satellite;
import seminars.exception.SpaceOperationException;
import seminars.factory.SatelliteFactory;
import seminars.factory.SatelliteParam;

import java.util.List;

/**
 * Реализация Strategy: вместо того чтобы вызывающий код сам решал, какую
 * фабрику использовать, сервис получает СПИСОК всех зарегистрированных
 * фабрик (каждая — отдельная "стратегия" создания спутника определённого
 * типа) и на основании SatelliteParam.getType() выбирает подходящую через
 * isSatelliteTypeSupported().
 *
 * Spring сам соберёт List<SatelliteFactory> из всех бинов, реализующих
 * этот интерфейс (CommunicationSatelliteFactory, ImagingSatelliteFactory,
 * и любые новые фабрики, которые появятся в будущем — без единой правки
 * этого класса, OCP).
 */
@Service
@RequiredArgsConstructor
public class SatelliteServiceImpl implements SatelliteService {

    private final List<SatelliteFactory> factories;

    @Override
    @LogExecutionTime
    public Satellite createSatellite(SatelliteParam param) {
        SatelliteFactory factory = factories.stream()
                .filter(candidate -> candidate.isSatelliteTypeSupported(param.getType()))
                .findFirst()
                .orElseThrow(() -> new SpaceOperationException(
                        "Не найдена фабрика, поддерживающая тип спутника: " + param.getType()));

        return factory.createSatelliteWithParameter(param);
    }
}
