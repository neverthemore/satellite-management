import java.util.ArrayList;
import java.util.List;

public class SatelliteConstellation {
    private String constellationName;
    private List<Satellite> satellites;

    public SatelliteConstellation(String constellationName) {
        this.constellationName = constellationName;
        this.satellites = new ArrayList<>();
        System.out.println("Создана спутниковая группировка: " + constellationName);
    }

    public void addSatellite(Satellite satellite) {
        satellites.add(satellite);
        System.out.println(satellite.getName() + " добавлен в группировку '" + constellationName + "'");
    }

    public void executeAllMissions() {
        System.out.println("ВЫПОЛНЕНИЕ МИССИЙ ГРУППИРОВКИ " + constellationName.toUpperCase());
        System.out.println("==================================================");
        for (Satellite satellite : satellites) {
            satellite.performMission();
        }
    }

    public List<Satellite> getSatellites() {
        return satellites;
    }

    public String getConstellationName() {
        return constellationName;
    }
}
