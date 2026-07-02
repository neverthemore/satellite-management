package seminars.service;

import seminars.domain.Satellite;
import seminars.factory.SatelliteParam;

/**
 * Единая точка создания спутников. Скрывает от вызывающего кода, какая
 * именно фабрика будет использована — нужно лишь передать SatelliteParam
 * нужного типа.
 */
public interface SatelliteService {
    Satellite createSatellite(SatelliteParam param);
}
