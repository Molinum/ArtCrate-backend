package cl.artcrate.catalogo.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Convierte todas las excepciones en una respuesta JSON uniforme.
 * basePackages limita su alcance a nuestros controladores (así no interfiere con Swagger).
 */
@RestControllerAdvice(basePackages = "cl.artcrate.catalogo.controller")
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Arma la respuesta de error con el formato {timestamp, status, error, message}. */
    private ResponseEntity<Object> responder(HttpStatusCode status, String mensaje) {
        String error = HttpStatus.valueOf(status.value()).getReasonPhrase();
        return ResponseEntity.status(status)
                .body(new ErrorRespuesta(LocalDateTime.now(), status.value(), error, mensaje));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> noEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Object> conflicto(ConflictoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Por si dos peticiones intentan guardar el mismo dato único a la vez: lo frena la restricción unique de la BD
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> integridad(DataIntegrityViolationException ex) {
        return responder(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro existente");
    }

    // Errores de @Valid: se juntan los mensajes de todos los campos inválidos
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    // JSON mal formado o con un valor que no corresponde (por ejemplo un rol inexistente)
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido o está incompleto");
    }

    // Cualquier otro error de Spring MVC (método no permitido, etc.) mantiene su código con el mismo formato
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return responder(statusCode, ex.getMessage());
    }

    // Último recurso: un error inesperado no debe mostrar detalles internos
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> inesperado(Exception ex) {
        log.error("Error inesperado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }
}
