public class Main {
    public static void main(String[] args) {

        System.out.println("ЗАПУСК СИСТЕМЫ УПРАВЛЕНИЯ СПУТНИКОВОЙ ГРУППИРОВКОЙ");
        System.out.println("============================================================");
        System.out.println("СОЗДАНИЕ СПЕЦИАЛИЗИРОВАННЫХ СПУТНИКОВ:");
        System.out.println("---------------------------------------------");

        CommunicationSatellite svyaz1 = new CommunicationSatellite("Связь-1", 0.85, 500.0);
        CommunicationSatellite svyaz2 = new CommunicationSatellite("Связь-2", 0.75, 1000.0);

        ImagingSatellite dzz1 = new ImagingSatellite("ДЗЗ-1", 0.92, 2.5);
        ImagingSatellite dzz2 = new ImagingSatellite("ДЗЗ-2", 0.45, 1.0);
        ImagingSatellite dzz3 = new ImagingSatellite("ДЗЗ-3", 0.15, 0.5);

        System.out.println("---------------------------------------------");

        SatelliteConstellation constellation = new SatelliteConstellation("RU Basic");

        System.out.println("---------------------------------------------");
        System.out.println("ФОРМИРОВАНИЕ ГРУППИРОВКИ:");
        System.out.println("-----------------------------------");

        constellation.addSatellite(svyaz1);
        constellation.addSatellite(svyaz2);
        constellation.addSatellite(dzz1);
        constellation.addSatellite(dzz2);
        constellation.addSatellite(dzz3);

        System.out.println("-----------------------------------");
        System.out.println(constellation.getSatellites());
        System.out.println("-----------------------------------");

        System.out.println("АКТИВАЦИЯ СПУТНИКОВ:");
        System.out.println("-------------------------");

        for (Satellite satellite : constellation.getSatellites()) {
            if (satellite.activate()) {
                System.out.println("✅ " + satellite.getName() + ": Активация успешна");
            } else {
                System.out.println("🛑 " + satellite.getName() + ": Ошибка активации (заряд: "
                        + (int)(satellite.getBatteryLevel() * 100) + "%)");
            }
        }

        constellation.executeAllMissions();
        System.out.println(constellation.getSatellites());
    }
}
