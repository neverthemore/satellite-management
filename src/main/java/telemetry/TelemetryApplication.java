package telemetry;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Микросервис телеметрии спутников.
 *
 * Запускает gRPC-сервер на порту 9091 (application.yml).
 * REST-эндпоинтов нет — общение только по gRPC.
 *
 * web(WebApplicationType.NONE) — явно отключает embedded Tomcat.
 * spring-boot-starter-web присутствует в общем classpath модуля (нужен
 * для seminars.Main), и без этой настройки Spring Boot по умолчанию
 * поднимал бы веб-сервер и здесь тоже — на том же порту 8080, что и
 * основной REST API, вызывая "Port 8080 was already in use" при
 * последующем запуске seminars.Main.
 *
 * ⚠️  ПЕРВЫЙ ЗАПУСК:
 *   ./gradlew generateProto   ← генерирует Java-код из telemetry.proto
 *   ./gradlew runTelemetry
 */
@SpringBootApplication
public class TelemetryApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(TelemetryApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
        System.out.println("╔═══════════════════════════════════════════════╗");
        System.out.println("║  Satellite Telemetry gRPC Server on port 9091  ║");
        System.out.println("╚═══════════════════════════════════════════════╝");
    }
}
