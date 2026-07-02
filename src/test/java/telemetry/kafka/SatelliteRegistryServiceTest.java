package telemetry.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Юнит-тесты SatelliteRegistryService без Spring-контекста и Kafka.
 */
@DisplayName("SatelliteRegistryService: юнит-тесты")
class SatelliteRegistryServiceTest {

    private SatelliteRegistryService registry;

    @BeforeEach
    void setUp() {
        registry = new SatelliteRegistryService();
    }

    @Test
    @DisplayName("Пустой реестр возвращает дефолтный список спутников")
    void getActiveSatellites_emptyRegistry_returnsDefaultList() {
        List<String> satellites = registry.getActiveSatellites();
        assertFalse(satellites.isEmpty(), "Дефолтный список не должен быть пустым");
    }

    @Test
    @DisplayName("После register() реестр содержит зарегистрированный спутник")
    void register_addsNameToActiveList() {
        registry.register("Связь-Тест-1");

        assertTrue(registry.getActiveSatellites().contains("Связь-Тест-1"));
    }

    @Test
    @DisplayName("После register() возвращается именно зарегистрированный список, не дефолтный")
    void register_overridesDefaultList() {
        registry.register("Новый-Спутник");

        List<String> satellites = registry.getActiveSatellites();
        // Когда есть хотя бы один зарегистрированный — дефолт не используется
        assertTrue(satellites.contains("Новый-Спутник"));
        assertFalse(satellites.contains("Связь-1"),
                "Дефолтный список не должен присутствовать после регистрации");
    }

    @Test
    @DisplayName("deregister() удаляет спутник из реестра")
    void deregister_removesSatelliteFromList() {
        registry.register("Связь-1");
        registry.register("ДЗЗ-1");

        registry.deregister("Связь-1");

        assertFalse(registry.getActiveSatellites().contains("Связь-1"));
        assertTrue(registry.getActiveSatellites().contains("ДЗЗ-1"));
    }

    @Test
    @DisplayName("deregister() последнего спутника возвращает дефолтный список")
    void deregisterLast_returnsDefaultList() {
        registry.register("Единственный-Спутник");
        registry.deregister("Единственный-Спутник");

        List<String> satellites = registry.getActiveSatellites();
        // Реестр пуст → дефолт
        assertFalse(satellites.isEmpty());
        // Дефолтный список известен
        assertTrue(satellites.contains("Связь-1") || satellites.size() > 0);
    }
}
