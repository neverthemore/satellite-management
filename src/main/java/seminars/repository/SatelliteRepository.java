package seminars.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import seminars.domain.Satellite;

import java.util.List;
import java.util.Optional;

@Repository
public interface SatelliteRepository extends JpaRepository<Satellite, Long> {

    Optional<Satellite> findByName(String name);

    List<Satellite> findByConstellationConstellationName(String constellationName);

    boolean existsByName(String name);

    List<Satellite> findByStateActiveTrue();
}