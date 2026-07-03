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
        // seminars.Main — REST API и gRPC-КЛИЕНТ (seminars.telemetry.TelemetryGrpcClient),
        // но grpc-server-spring-boot-starter тоже присутствует в общем classpath модуля
        // (он нужен только telemetry.TelemetryApplication). Без этой настройки его
        // автоконфигурация всё равно поднимает embedded gRPC-сервер (с встроенными
        // Health/Reflection-сервисами, у Main нет ни одного @GrpcService) на порту
        // grpc.server.port из application.yaml — том же 9091, что уже слушает
        // запущенный telemetry.TelemetryApplication — и падает с
        // "Address already in use: bind". "-1" — официально документированное
        // значение net.devh grpc-server-spring-boot-starter для полного отключения
        // сервера (см. GrpcServerProperties#port), Main он не нужен вовсе.
        System.setProperty("grpc.server.port", "-1");

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
