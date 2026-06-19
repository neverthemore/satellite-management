package seminars.domain;

public class CommunicationSatellite extends Satellite {
    private static final double MISSION_ENERGY_COST = 0.05;

    private double bandWidth;

    public CommunicationSatellite(String name, double batteryLevel, double bandWidth) {
        super(name, batteryLevel);
        this.bandWidth = bandWidth;
    }

    public double getBandwidth() {
        return bandWidth;
    }

    @Override
    protected void performMission() {
        if (state.isActive()) {
            System.out.println(name + ": Передача данных со скоростью " + bandWidth + " Мбит/с");
            sendData(bandWidth);

            energy.consume(MISSION_ENERGY_COST);
            if (energy.isCritical()) {
                state.deactivate();
            }
        }
    }

    public void sendData(double amount) {
        if (state.isActive()) {
            System.out.println(name + ": Отправил " + amount + " Мбит данных!");
        }
    }

    @Override
    public String toString() {
        return "CommunicationSatellite{" +
                "bandwidth=" + bandWidth +
                ", name='" + name + "'" +
                ", state=" + state +
                ", energy=" + energy +
                "}";
    }
}
