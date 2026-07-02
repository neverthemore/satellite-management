package seminars.factory;

import org.springframework.stereotype.Component;
import seminars.domain.CommunicationSatellite;
import seminars.domain.Satellite;
import seminars.exception.SpaceOperationException;

/**
 * Конкретная фабрика спутников связи.
 * Зарегистрирована как Spring-бин — попадает в List<SatelliteFactory>,
 * который внедряется в SatelliteServiceImpl.
 */
@Component
public class CommunicationSatelliteFactory implements SatelliteFactory {

    @Override
    public Satellite createSatelliteWithParameter(SatelliteParam param) {
        if (!(param instanceof CommunicationSatelliteParam communicationParam)) {
            throw new SpaceOperationException(
                    "CommunicationSatelliteFactory не поддерживает параметр типа: "
                            + describeParamType(param));
        }
        return new CommunicationSatellite(
                communicationParam.getName(),
                communicationParam.getBatteryLevel(),
                communicationParam.getBandwidth());
    }

    @Override
    public boolean isSatelliteTypeSupported(SatelliteType type) {
        return SatelliteType.COMMUNICATION == type;
    }

    private static String describeParamType(SatelliteParam param) {
        return param == null ? "null" : param.getClass().getSimpleName();
    }
}
