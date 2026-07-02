package seminars.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import seminars.kafka.TestKafkaConfig;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import seminars.Main;
import seminars.facade.AddSatelliteRequest;
import seminars.facade.MissionRequest;
import seminars.factory.CommunicationSatelliteParam;
import seminars.factory.ImagingSatelliteParam;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты SpaceOperationController через MockMvc.
 *
 * MockMvc поднимает полный Spring Web MVC стек (сериализация/десериализация,
 * обработчики ошибок, маппинг URL), но без реального HTTP-сервера —
 * это быстрее, чем @SpringBootTest с webEnvironment=RANDOM_PORT,
 * но достаточно для проверки контроллера.
 *
 * @AutoConfigureMockMvc настраивает MockMvc автоматически.
 */
@ActiveProfiles("test")
@Import(TestKafkaConfig.class)
@SpringBootTest(classes = Main.class)
@AutoConfigureMockMvc
@DisplayName("SpaceOperationController: интеграционные тесты через MockMvc")
class SpaceOperationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CONSTELLATION_NAME = "Контроллер-Орбита";

    @Test
    @DisplayName("POST /api/add-satellites — добавляет спутники и возвращает их имена")
    void addSatellites_validRequest_returnsAddedSatelliteNames() throws Exception {
        AddSatelliteRequest request = AddSatelliteRequest.builder()
                .constellationName(CONSTELLATION_NAME + "-add")
                .satelliteParam(new CommunicationSatelliteParam("Связь-REST-1", 0.8, 500.0))
                .satelliteParam(new ImagingSatelliteParam("ДЗЗ-REST-1", 0.9, 2.5))
                .build();

        mockMvc.perform(post("/api/add-satellites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(content().string(containsString("Связь-REST-1")))
                .andExpect(content().string(containsString("ДЗЗ-REST-1")));
    }

    @Test
    @DisplayName("POST /api/missions — выполняет миссию и возвращает 200 OK")
    void executeMission_existingConstellation_returns200() throws Exception {
        String constellationName = CONSTELLATION_NAME + "-mission";

        // Сначала создаём группировку со спутниками
        AddSatelliteRequest addRequest = AddSatelliteRequest.builder()
                .constellationName(constellationName)
                .satelliteParam(new CommunicationSatelliteParam("Связь-REST-М", 0.8, 500.0))
                .build();

        mockMvc.perform(post("/api/add-satellites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());

        // Затем выполняем миссию
        MissionRequest missionRequest = MissionRequest.builder()
                .constellationName(constellationName)
                .activateBeforeMission(true)
                .build();

        mockMvc.perform(post("/api/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missionRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/deploy — полный цикл развёртывания, возвращает имена спутников")
    void deployConstellation_validRequest_returnsDeployedSatelliteNames() throws Exception {
        AddSatelliteRequest request = AddSatelliteRequest.builder()
                .constellationName(CONSTELLATION_NAME + "-deploy")
                .satelliteParam(new ImagingSatelliteParam("ДЗЗ-REST-D", 0.9, 1.5))
                .build();

        mockMvc.perform(post("/api/deploy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(content().string(containsString("ДЗЗ-REST-D")));
    }

    @Test
    @DisplayName("GET /api/overview — возвращает сводку по всем группировкам")
    void getOverview_returnsSystemSummary() throws Exception {
        mockMvc.perform(get("/api/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConstellations").isNumber());
    }

    @Test
    @DisplayName("GET /api/constellations/{name}/report — возвращает отчёт по существующей группировке")
    void getConstellationReport_existingConstellation_returnsReport() throws Exception {
        String constellationName = CONSTELLATION_NAME + "-report";

        AddSatelliteRequest addRequest = AddSatelliteRequest.builder()
                .constellationName(constellationName)
                .satelliteParam(new CommunicationSatelliteParam("Связь-REST-R", 0.7, 400.0))
                .build();

        mockMvc.perform(post("/api/add-satellites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/constellations/{name}/report", constellationName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.constellationName").value(constellationName))
                .andExpect(jsonPath("$.totalSatellites").value(1))
                .andExpect(jsonPath("$.activeSatellites").value(0));
    }

    @Test
    @DisplayName("GET /api/constellations/{name}/report — 422 для несуществующей группировки")
    void getConstellationReport_unknownConstellation_returns422() throws Exception {
        mockMvc.perform(get("/api/constellations/{name}/report", "НесуществующаяГруппировка"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("DELETE /api/constellations/{name}/satellites/{satName} — деактивирует спутник")
    void decommissionSatellite_existingSatellite_returns200() throws Exception {
        String constellationName = CONSTELLATION_NAME + "-decom";
        String satelliteName = "Связь-REST-Dec";

        AddSatelliteRequest addRequest = AddSatelliteRequest.builder()
                .constellationName(constellationName)
                .satelliteParam(new CommunicationSatelliteParam(satelliteName, 0.8, 500.0))
                .build();

        mockMvc.perform(post("/api/add-satellites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/constellations/{con}/satellites/{sat}",
                        constellationName, satelliteName))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("деактивирован")));
    }

    @Test
    @DisplayName("DELETE /api/constellations/{name}/satellites/{satName} — 404 для несуществующего спутника")
    void decommissionSatellite_unknownSatellite_returns404() throws Exception {
        mockMvc.perform(delete("/api/constellations/{con}/satellites/{sat}",
                        "НесуществующаяГруппировка", "НесуществующийСпутник"))
                .andExpect(status().isNotFound());
    }
}
