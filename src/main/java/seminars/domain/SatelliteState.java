package seminars.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.ToString;

/**
 * Состояние активности спутника — @Embeddable (те же соображения, что для EnergySystem).
 * JPA требует protected no-arg конструктор.
 */
@Embeddable
@Getter
@ToString
public class SatelliteState {

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "status_message", nullable = false)
    private String statusMessage;

    protected SatelliteState() {
        this.active = false;
        this.statusMessage = "Не активирован";
    }

    public void activate() {
        this.active = true;
        this.statusMessage = "Активен";
    }

    public void deactivate() {
        this.active = false;
        this.statusMessage = "Деактивирован";
    }

    public boolean isActive() {
        return active;
    }
}
