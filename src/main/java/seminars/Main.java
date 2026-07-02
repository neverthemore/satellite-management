package seminars;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import seminars.facade.AddSatelliteRequest;
import seminars.facade.ConstellationStatusReport;
import seminars.facade.MissionRequest;
import seminars.facade.SpaceOperationCenterService;
import seminars.factory.CommunicationSatelliteParam;
import seminars.factory.ImagingSatelliteParam;
import seminars.repository.SatelliteConstellationRepository;
import seminars.service.ConstellationService;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        System.out.println("ЗАПУСК СИСТЕМЫ УПРАВЛЕНИЯ СПУТНИКОВОЙ ГРУППИРОВКОЙ");
        System.out.println("============================================================");

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);

        // Получаем бины из Spring-контейнера
        // ConstellationRepository (HashMap) больше не существует —
        // данные теперь в PostgreSQL через SatelliteConstellationRepository
        SatelliteConstellationRepository constellationRepository =
                context.getBean(SatelliteConstellationRepository.class);
        ConstellationService constellationService = context.getBean(ConstellationService.class);
        SpaceOperationCenterService operationCenter = context.getBean(SpaceOperationCenterService.class);

        System.out.println();
        System.out.println("ФОРМИРОВАНИЕ ГРУППИРОВОК ЧЕРЕЗ ФАСАД:");
        System.out.println("---------------------------------------------");

        operationCenter.addSatellite(AddSatelliteRequest.builder()
                .constellationName("Орбита-1")
                .satelliteParam(new CommunicationSatelliteParam("Связь-1", 0.85, 500.0))
                .satelliteParam(new ImagingSatelliteParam("ДЗЗ-1", 0.92, 2.5))
                .satelliteParam(new ImagingSatelliteParam("ДЗЗ-2", 0.45, 1.0))
                .build());

        operationCenter.addSatellite(AddSatelliteRequest.builder()
                .constellationName("Орбита-2")
                .satelliteParam(new CommunicationSatelliteParam("Связь-2", 0.75, 1000.0))
                .satelliteParam(new ImagingSatelliteParam("ДЗЗ-3", 0.15, 0.5))
                .build());

        System.out.println("-----------------------------------");

        constellationService.showConstellationStatus("Орбита-1");

        operationCenter.executeMission(MissionRequest.builder()
                .constellationName("Орбита-1")
                .build());

        constellationService.showConstellationStatus("Орбита-1");

        ConstellationStatusReport report = operationCenter.getConstellationReport("Орбита-1");
        System.out.println();
        System.out.println("Сводка по Орбита-1: " + report);

        // Все группировки теперь в PostgreSQL — при следующем запуске данные сохранятся
        System.out.println();
        System.out.println(constellationRepository.findAll());
    }
}
