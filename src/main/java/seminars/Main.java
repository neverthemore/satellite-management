package seminars;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import seminars.domain.CommunicationSatellite;
import seminars.domain.ImagingSatellite;
import seminars.repository.ConstellationRepository;
import seminars.service.SpaceOperationCenterService;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        System.out.println("ЗАПУСК СИСТЕМЫ УПРАВЛЕНИЯ СПУТНИКОВОЙ ГРУППИРОВКОЙ");
        System.out.println("============================================================");

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);

        // Достаём бины, созданные и связанные Spring-контейнером (DIP в действии:
        // мы не пишем new ConstellationRepository() / new SpaceOperationCenterService(...) —
        // готовые экземпляры со всеми зависимостями нам отдаёт контейнер)
        ConstellationRepository constellationRepository = context.getBean(ConstellationRepository.class);
        SpaceOperationCenterService operationCenter = context.getBean(SpaceOperationCenterService.class);

        System.out.println();
        System.out.println("СОЗДАНИЕ СПЕЦИАЛИЗИРОВАННЫХ СПУТНИКОВ:");
        System.out.println("---------------------------------------------");

        // Доменные объекты спутников создаются напрямую через конструктор —
        // у каждого свои параметры, Spring ими не управляет
        CommunicationSatellite svyaz1 = new CommunicationSatellite("Связь-1", 0.85, 500.0);
        CommunicationSatellite svyaz2 = new CommunicationSatellite("Связь-2", 0.75, 1000.0);
        ImagingSatellite dzz1 = new ImagingSatellite("ДЗЗ-1", 0.92, 2.5);
        ImagingSatellite dzz2 = new ImagingSatellite("ДЗЗ-2", 0.45, 1.0);
        ImagingSatellite dzz3 = new ImagingSatellite("ДЗЗ-3", 0.15, 0.5);

        System.out.println("---------------------------------------------");

        // Создание группировок — через сервис, а не напрямую через репозиторий
        operationCenter.createAndSaveConstellation("Орбита-1");
        operationCenter.createAndSaveConstellation("Орбита-2");

        System.out.println("---------------------------------------------");
        System.out.println();
        System.out.println("📡 ДОБАВЛЕНИЕ СПУТНИКОВ:");

        operationCenter.addSatelliteToConstellation("Орбита-1", svyaz1);
        operationCenter.addSatelliteToConstellation("Орбита-1", dzz1);
        operationCenter.addSatelliteToConstellation("Орбита-1", dzz2);
        operationCenter.addSatelliteToConstellation("Орбита-2", svyaz2);
        operationCenter.addSatelliteToConstellation("Орбита-2", dzz3);

        System.out.println("-----------------------------------");

        // Проверка начальных статусов — до активации
        operationCenter.showConstellationStatus("Орбита-1");

        // Активация спутников в группировке
        operationCenter.activateAllSatellites("Орбита-1");

        // Выполнение миссий
        operationCenter.executeConstellationMission("Орбита-1");

        // Проверка итоговых статусов — после выполнения миссий
        operationCenter.showConstellationStatus("Орбита-1");

        System.out.println();
        System.out.println(constellationRepository.findAll());
    }
}
