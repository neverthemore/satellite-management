public class ImagingSatellite extends Satellite {
    private double resolution;
    private int photosTaken;

    public ImagingSatellite(String name, double batteryLevel, double resolution) {
        super(name, batteryLevel);
        this.resolution = resolution;
        this.photosTaken = 0;
    }

    public double getResolution() {
        return resolution;
    }

    public int getPhotosTaken() {
        return photosTaken;
    }

    @Override
    protected void performMission() {
        if (isActive) {
            System.out.println(name + ": Съемка территории с разрешением " + resolution + " м/пиксель");
            takePhoto();
            consumeBattery(0.08);
        } else {
            System.out.println("🛑 " + name + ": Не может выполнить съемку - не активен");
        }
    }

    public void takePhoto() {
        if (isActive) {
            photosTaken++;
            System.out.println(name + ": Снимок #" + photosTaken + " сделан!");
        }
    }

    @Override
    public String toString() {
        return "ImagingSatellite{" +
                "resolution=" + resolution +
                ", photosTaken=" + photosTaken +
                ", name='" + name + "'" +
                ", isActive=" + isActive +
                ", batteryLevel=" + batteryLevel +
                "}";
    }
}
