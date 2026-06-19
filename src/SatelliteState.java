/**
 * Отвечает ТОЛЬКО за управление состоянием активности спутника.
 * Реализует принцип единственной ответственности (SRP).
 */
public class SatelliteState {
    private boolean active;

    public SatelliteState() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }
}
