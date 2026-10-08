package cl.artcrate.catalogo.exception;

import java.time.LocalDateTime;

/** Formato único de todos los errores de la API: {timestamp, status, error, message}. */
public record ErrorRespuesta(
        LocalDateTime timestamp,
        int status,
        String error,
        String message) {
}
