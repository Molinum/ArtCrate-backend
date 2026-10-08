package cl.artcrate.carrito.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.RetryableException;
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
@RestControllerAdvice(basePackages = "cl.artcrate.carrito.controller")
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);
    private static final ObjectMapper JSON = new ObjectMapper();

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> integridad(DataIntegrityViolationException ex) {
        return responder(HttpStatus.CONFLICT, "Los datos entran en conflicto con un registro existente");
    }

    // ----- Errores al llamar a otro servicio con Feign -----

    // El otro servicio NO responde (apagado, conexión rechazada o más de 5 segundos): 503
    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<Object> servicioNoDisponible(RetryableException ex) {
        log.warn("No se pudo conectar con el servicio {}: {}", nombreServicio(ex), ex.getMessage());
        return responder(HttpStatus.SERVICE_UNAVAILABLE, "Servicio " + nombreServicio(ex) + " no disponible");
    }

    // El otro servicio respondió con un error: 404, 409 y demás errores del cliente se devuelven
    // con su mismo código y mensaje; los errores 5xx significan que ese servicio está fallando (503)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> errorDeOtroServicio(FeignException ex) {
        if (ex.status() >= 400 && ex.status() < 500) {
            return responder(HttpStatus.valueOf(ex.status()), mensajeDelOtroServicio(ex));
        }
        log.warn("El servicio {} respondió con error {}", nombreServicio(ex), ex.status());
        return responder(HttpStatus.SERVICE_UNAVAILABLE, "Servicio " + nombreServicio(ex) + " no disponible");
    }

    /** Saca el "message" del JSON de error del otro servicio (que usa nuestro mismo formato). */
    private String mensajeDelOtroServicio(FeignException ex) {
        try {
            JsonNode json = JSON.readTree(ex.contentUTF8());
            if (json.hasNonNull("message")) {
                return json.get("message").asText();
            }
        } catch (Exception e) {
            // si la respuesta no era JSON se usa el mensaje genérico de abajo
        }
        return "Error en el servicio " + nombreServicio(ex);
    }

    /** Deduce a qué servicio se llamó mirando la dirección de la petición. */
    private String nombreServicio(FeignException ex) {
        String url = (ex.request() != null) ? ex.request().url() : "";
        if (url.contains("/api/inventario")) return "inventario";
        if (url.contains("/api/productos")) return "catalogo";
        if (url.contains("/api/carritos")) return "carrito";
        if (url.contains("/api/pedidos")) return "pedidos";
        return "externo";
    }

    // ----- Errores de validación y de Spring MVC -----

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido o está incompleto");
    }

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
