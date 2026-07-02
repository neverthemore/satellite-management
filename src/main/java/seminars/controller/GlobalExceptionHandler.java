package seminars.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import seminars.exception.SpaceOperationException;

import java.time.Instant;
import java.util.Map;

/**
 * Перехватывает исключения из сервисного слоя и превращает их в
 * структурированные JSON-ответы с правильными HTTP-кодами — это стандартная
 * практика, позволяющая отделить транспортный слой от бизнес-логики.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SpaceOperationException.class)
    public ResponseEntity<Map<String, Object>> handleSpaceOperationException(SpaceOperationException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                "error", "Space operation failed",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Resource not found",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
