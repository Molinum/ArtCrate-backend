package cl.artcrate.carrito.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente Feign de catalogo-service. Solo se escribe la interfaz: Feign genera el código
 * que hace la petición HTTP. La URL base sale de application.properties (servicios.catalogo.url).
 */
@FeignClient(name = "catalogo-service", url = "${servicios.catalogo.url}")
public interface CatalogoClient {

    @GetMapping("/api/productos/{id}")
    ProductoCliente obtenerProducto(@PathVariable("id") Long id);
}
