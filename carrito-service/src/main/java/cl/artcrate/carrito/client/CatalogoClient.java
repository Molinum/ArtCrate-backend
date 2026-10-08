package cl.artcrate.carrito.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign de catalogo-service. Solo se escribe la interfaz: Feign genera el código
 * que hace la petición HTTP. "name" es el spring.application.name del servicio destino:
 * Eureka dice en qué dirección está, así que no hace falta escribir ninguna URL.
 */
@FeignClient(name = "catalogo-service")
public interface CatalogoClient {

    @GetMapping("/api/productos/{id}")
    ProductoCliente obtenerProducto(@PathVariable("id") Long id);
}
