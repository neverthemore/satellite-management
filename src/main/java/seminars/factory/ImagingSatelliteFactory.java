package seminars.factory;

import org.springframework.stereotype.Component;
import seminars.domain.ImagingSatellite;
import seminars.domain.Satellite;
import seminars.exception.SpaceOperationException;

/**
 * Конкретная фабрика спутников ДЗЗ.
 * Зарегистрирована как Spring-бин — попадает в List<SatelliteFactory>,
 * который внедряется в SatelliteServiceImpl.
 */
@Component
public class ImagingSatelliteFactory implements SatelliteFactory {

    @Override
    public Satellite createSatelliteWithParameter(SatelliteParam param) {
        if (!(param instanceof ImagingSatelliteParam imagingParam)) {
            throw new SpaceOperationException(
                    "ImagingSatelliteFactory не поддерживает параметр типа: "
                            + describeParamType(param));
        }
        return new ImagingSatellite(
                imagingParam.getName(),
                imagingParam.getBatteryLevel(),
                imagingParam.getResolution());
    }

    @Override
    public boolean isSatelliteTypeSupported(SatelliteType type) {
        return SatelliteType.IMAGE == type;
    }

    private static String describeParamType(SatelliteParam param) {
        return param == null ? "null" : param.getClass().getSimpleName();
    }
}
