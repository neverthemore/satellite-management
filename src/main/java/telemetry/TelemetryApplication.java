package telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Микросервис телеметрии спутников.
 *
 * Запускает gRPC-сервер на порту 9091 (application.yml).
 * REST-эндпоинтов нет — общение только по gRPC.
 *
 * ⚠️  ПЕРВЫЙ ЗАПУСК:
 *   ./gradlew generateProto   ← генерирует Java-код из telemetry.proto
 *   ./gradlew bootRun
 */
@SpringBootApplication
public class TelemetryApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelemetryApplication.class, args);
        System.out.println("╔═══════════════════════════════════════════════╗");
        System.out.println("║  Satellite Telemetry gRPC Server on port 9091  ║");
        System.out.println("╚═══════════════════════════════════════════════╝");
    }
}
