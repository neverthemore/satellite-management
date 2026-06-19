package seminars.repository;

import org.springframework.stereotype.Repository;
import seminars.domain.SatelliteConstellation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Хранилище спутниковых группировок.
 *
 * Сейчас данные живут в памяти (Map), но интерфейс CRUD-операций спроектирован
 * так, чтобы позже легко подменить реализацию на работу с реальной БД
 * (например, JpaRepository), не трогая код, который этим репозиторием пользуется —
 * это и есть смысл Dependency Inversion: сервис зависит от абстракции репозитория,
 * а не от конкретного способа хранения.
 */
@Repository
public class ConstellationRepository {

    private final Map<String, SatelliteConstellation> constellations = new HashMap<>();

    public SatelliteConstellation save(SatelliteConstellation constellation) {
        constellations.put(constellation.getConstellationName(), constellation);
        System.out.println("Сохранена группировка: " + constellation.getConstellationName());
        return constellation;
    }

    public Optional<SatelliteConstellation> findByName(String name) {
        return Optional.ofNullable(constellations.get(name));
    }

    public Map<String, SatelliteConstellation> findAll() {
        return constellations;
    }

    public boolean existsByName(String name) {
        return constellations.containsKey(name);
    }

    public void deleteByName(String name) {
        constellations.remove(name);
    }
}
