package scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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
 * Сервис запускается на порту 8081 (application.yml), чтобы работать
 * одновременно с основным сервисом на 8080.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SpaceCenterProperties.class)
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Mission Scheduler запущен на порту 8081             ║");
        System.out.println("║  Основной сервис ожидается на http://localhost:8080  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
}
