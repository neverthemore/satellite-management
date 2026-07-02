package seminars.factory;

import seminars.domain.Satellite;

/**
 * Фабричный метод (Factory Method, GoF) для создания спутников.
 *
 * Каждая конкретная фабрика инкапсулирует логику создания продукта
 * определённого типа и умеет сообщить, какой тип она обслуживает —
 * это и используется сервисом (SatelliteServiceImpl) для выбора нужной
 * "стратегии" создания среди всех зарегистрированных фабрик.
 */
public interface SatelliteFactory {

    /** Создаёт спутник на основе унифицированного параметра. */
    Satellite createSatelliteWithParameter(SatelliteParam param);

    /** Поддерживает ли эта фабрика создание спутников указанного типа. */
    boolean isSatelliteTypeSupported(SatelliteType type);
}
