package seminars.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import seminars.domain.SatelliteConstellation;

import java.util.Optional;

/**
 * JPA-репозиторий для SatelliteConstellation.
 *
 * Spring Data JPA автоматически генерирует реализацию методов
 * по соглашению об именовании: findBy<Поле>(<тип>) → SELECT ... WHERE поле = ?
 *
 * Заменяет старый HashMap-основанный ConstellationRepository.
 */
@Repository
public interface SatelliteConstellationRepository extends JpaRepository<SatelliteConstellation, Long> {

    Optional<SatelliteConstellation> findByConstellationName(String constellationName);

    boolean existsByConstellationName(String constellationName);

    void deleteByConstellationName(String constellationName);

    /**
     * Загружает группировку вместе со спутниками одним запросом (JOIN FETCH),
     * избегая N+1 проблемы при обращении к lazy-коллекции satellites.
     *
     * Без этого каждый доступ к constellation.getSatellites() при незагруженной
     * коллекции вызвал бы дополнительный SELECT.
     */
    @Query("SELECT c FROM SatelliteConstellation c LEFT JOIN FETCH c.satellites WHERE c.constellationName = :name")
    Optional<SatelliteConstellation> findByConstellationNameWithSatellites(String name);
}
