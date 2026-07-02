package seminars.aop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import seminars.kafka.TestKafkaConfig;
import org.springframework.test.context.ActiveProfiles;
import seminars.Main;
import seminars.factory.CommunicationSatelliteParam;
import seminars.service.SatelliteService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет, что ExecutionTimeAspect реально перехватывает вызовы методов,
 * помеченных @LogExecutionTime, через настоящий Spring AOP прокси —
 * не мокаем аспект, а ловим его побочный эффект (вывод в консоль).
 *
 * SatelliteServiceImpl.createSatellite() выбран намеренно: вызов идёт
 * СНАРУЖИ бина (из тестового класса, который сам бином не является),
 * поэтому проходит через прокси и аспект гарантированно сработает —
 * в отличие от self-invocation внутри одного бина.
 */
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@SpringBootTest(classes = Main.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("LogExecutionTime / ExecutionTimeAspect: интеграционный тест")
class ExecutionTimeAspectTest {

    private static final String SATELLITE_NAME = "Связь-Аспект-1";
    private static final double SATELLITE_BATTERY_LEVEL = 0.7;
    private static final double SATELLITE_BANDWIDTH = 350.0;

    @Autowired
    private SatelliteService satelliteService;

    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void redirectSystemOut() {
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true));
    }

    @AfterEach
    void restoreSystemOut() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("Вызов метода с @LogExecutionTime печатает в консоль строку с замером времени выполнения")
    void createSatellite_annotatedMethod_logsExecutionTimeToConsole() {
        satelliteService.createSatellite(
                new CommunicationSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, SATELLITE_BANDWIDTH));

        String output = capturedOut.toString();
        assertTrue(output.contains("выполнен за"));
        assertTrue(output.contains("мс"));
    }

    @Test
    @DisplayName("Замер времени появляется в выводе только один раз на один вызов аннотированного метода")
    void createSatellite_singleCall_logsExactlyOnce() {
        satelliteService.createSatellite(
                new CommunicationSatelliteParam(SATELLITE_NAME, SATELLITE_BATTERY_LEVEL, SATELLITE_BANDWIDTH));

        String output = capturedOut.toString();
        int occurrences = output.split("выполнен за", -1).length - 1;
        assertTrue(occurrences == 1, "Ожидался ровно один замер, найдено: " + occurrences);
    }

    @Test
    @DisplayName("Вызов НЕаннотированного метода не оставляет в консоли строку с замером времени")
    void unannotatedCall_doesNotLogExecutionTime() {
        // toString() ни от какого бина не вызывается через прокси и не помечен аннотацией —
        // подходящий пример "молчаливого" вызова без побочного эффекта аспекта
        String ignored = satelliteService.toString();

        String output = capturedOut.toString();
        assertFalse(output.contains("выполнен за"));
    }
}
