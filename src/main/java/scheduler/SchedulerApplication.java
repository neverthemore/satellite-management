package scheduler;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import scheduler.properties.SpaceCenterProperties;

/**
 * Микросервис-планировщик миссий.
 *
 * @EnableScheduling — активирует инфраструктуру планировщика задач Spring:
 *   создаётся ThreadPoolTaskScheduler (бин типа TaskScheduler), который
 *   ConfiguredMissionScheduler использует для регистрации cron-задач.
 *
 * @EnableConfigurationProperties — регистрирует SpaceCenterProperties как бин
 *   и запускает его заполнение из application.yml.
 *
 * У scheduler-пакета нет ни одного @RestController — он только ходит
 * наружу (RestClient в SpaceOperationClient) и выполняет cron-задачи.
 * web(WebApplicationType.NONE) явно отключает embedded Tomcat: без этого
 * Spring Boot поднял бы веб-сервер и здесь тоже (spring-boot-starter-web
 * есть в общем classpath модуля), на том же порту 8080 по умолчанию, что
 * и seminars.Main — конфликт "Port 8080 was already in use".
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SpaceCenterProperties.class)
public class SchedulerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SchedulerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Mission Scheduler запущен                            ║");
        System.out.println("║  Основной сервис ожидается на http://localhost:8080  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
}
