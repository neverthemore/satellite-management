public class CommunicationSatellite extends Satellite {
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
        if (isActive) {
            System.out.println(name + ": Передача данных со скоростью " + bandWidth + " Мбит/с");
            sendData(bandWidth);
            consumeBattery(0.05);
        }
    }

    public void sendData(double amount) {
        if (isActive) {
            System.out.println(name + ": Отправил " + amount + " Мбит данных!");
        }
    }

    @Override
    public String toString() {
        return "CommunicationSatellite{" +
                "bandwidth=" + bandWidth +
                ", name='" + name + "'" +
                ", isActive=" + isActive +
                ", batteryLevel=" + batteryLevel +
                "}";
    }
}
