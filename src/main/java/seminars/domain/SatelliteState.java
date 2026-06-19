package seminars.domain;

/**
 * Отвечает ТОЛЬКО за управление состоянием активности спутника.
 * Реализует принцип единственной ответственности (SRP).
 */
public class SatelliteState {
    private boolean active;
    private String statusMessage;

    public SatelliteState() {
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

    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public String toString() {
        return "SatelliteState{isActive=" + active + ", statusMessage='" + statusMessage + "'}";
    }
}
