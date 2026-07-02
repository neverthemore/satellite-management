package seminars.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import seminars.kafka.TestKafkaConfig;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import seminars.Main;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc тест TelemetryController.
 * @ActiveProfiles("test")
@Import(TestKafkaConfig.class) — H2 + telemetry.client.enabled=false.
 */
@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@DisplayName("TelemetryController: интеграционные тесты")
class TelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/telemetry — возвращает пустой список, если спутников нет")
    void getAllTelemetry_emptySatellites_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/telemetry")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/telemetry/{id} — 404 для несуществующего ID")
    void getTelemetry_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/telemetry/{id}", 999999L))
                .andExpect(status().isNotFound());
    }
}
