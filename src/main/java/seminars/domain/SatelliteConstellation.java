package seminars.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA-сущность спутниковой группировки.
 *
 * @OneToMany(cascade = ALL): когда группировка сохраняется,
 * добавленные в неё спутники каскадно сохраняются/обновляются.
 * orphanRemoval = true: спутник, удалённый из satellites, удаляется из БД.
 *
 * @JsonManagedReference: включаем satellites в JSON-ответ;
 * обратная сторона (Satellite.constellation) помечена @JsonBackReference
 * и в JSON не включается — разрываем кольцевую ссылку.
 */
@Entity
@Table(name = "satellite_constellations")
@Getter
@ToString(exclude = "satellites")
public class SatelliteConstellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "constellation_name", nullable = false, unique = true)
    private String constellationName;

    @OneToMany(mappedBy = "constellation", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private List<Satellite> satellites = new ArrayList<>();

    /** Для JPA. */
    protected SatelliteConstellation() {}

    public SatelliteConstellation(String constellationName) {
        this.constellationName = constellationName;
        System.out.println("Создана спутниковая группировка: " + constellationName);
    }

    /**
     * Добавляет спутник в группировку, устанавливая обе стороны
     * двунаправленной связи (@ManyToOne / @OneToMany).
     * Без satellite.setConstellation(this) Hibernate не запишет FK constellation_id.
     */
    public void addSatellite(Satellite satellite) {
        satellites.add(satellite);
        satellite.setConstellation(this);
        System.out.println(satellite.getName() + " добавлен в группировку '" + constellationName + "'");
    }

    public void executeAllMissions() {
        System.out.println("ВЫПОЛНЕНИЕ МИССИЙ ГРУППИРОВКИ " + constellationName.toUpperCase());
        System.out.println("==================================================");
        for (Satellite satellite : satellites) {
            satellite.performMission();
        }
    }
}
