package cl.artcrate.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Maneja los errores del PROPIO gateway (no los de los servicios: esos pasan tal cual).
 * Todos salen con el mismo formato JSON que usan los servicios: {timestamp, status, error, message}.
 *
 *   - Servicio caído, sin instancias en Eureka o que no responde a tiempo -> 503 "Servicio X no disponible"
 *   - Ruta que no existe                                                  -> 404
 *   - Cualquier otro error inesperado                                     -> 500
 *
 * @Order(-2) hace que este manejador se use ANTES que el que trae Spring por defecto (que tiene -1).
 */
@Component
@Order(-2)
public class ManejadorErroresGateway implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresGateway.class);

    private final ObjectMapper mapper;

    public ManejadorErroresGateway(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex); // ya se empezó a responder: no se puede cambiar nada
        }

        HttpStatus status;
        String mensaje;

        if (esServicioNoDisponible(ex)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            mensaje = "Servicio " + nombreServicio(exchange) + " no disponible";
            log.warn("{} {} -> {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath(), mensaje);
        } else if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            mensaje = (status == HttpStatus.NOT_FOUND)
                    ? "No existe la ruta " + exchange.getRequest().getURI().getPath()
                    : (rse.getReason() != null ? rse.getReason() : status.getReasonPhrase());
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            mensaje = "Error interno del servidor";
            log.error("Error inesperado en el gateway", ex);
        }

        // Se arma el mismo JSON que devuelven los servicios
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now().toString());
        cuerpo.put("status", status.value());
        cuerpo.put("error", status.getReasonPhrase());
        cuerpo.put("message", mensaje);

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] bytes = mapper.writeValueAsBytes(cuerpo);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * ¿El problema es que el servicio de destino no está disponible?
     * Cubre: no hay instancias en Eureka (el balanceador responde 503), conexión rechazada
     * (el servicio acaba de caer y Eureka todavía no lo elimina) y tiempos de espera agotados.
     */
    private boolean esServicioNoDisponible(Throwable ex) {
        if (ex instanceof ResponseStatusException rse && rse.getStatusCode().value() == 503) {
            return true;
        }
        // Se revisa también la causa de la causa (las excepciones de red vienen envueltas en otras)
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof IOException || t.getClass().getSimpleName().contains("Timeout")) {
                return true; // ConnectException, conexión cerrada, timeout, etc.
            }
        }
        return false;
    }

    /** El id de la ruta (por ejemplo "inventario") es el nombre que se muestra en el mensaje. */
    private String nombreServicio(ServerWebExchange exchange) {
        Route ruta = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return (ruta != null) ? ruta.getId() : "externo";
    }
}
