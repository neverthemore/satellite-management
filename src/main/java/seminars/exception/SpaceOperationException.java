package seminars.exception;

/**
 * Выбрасывается при ошибках в операциях управления космической системой:
 * например, когда фабрике передан параметр неподдерживаемого типа, или
 * когда сервису не удалось найти подходящую фабрику.
 *
 * Unchecked (наследник RuntimeException) — методы интерфейсов SatelliteFactory
 * и SatelliteService остаются чистыми, без throws-деклараций.
 */
public class SpaceOperationException extends RuntimeException {

    public SpaceOperationException(String message) {
        super(message);
    }

    public SpaceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
