package cl.artcrate.pedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Cliente Feign de carrito-service (se encuentra por su nombre en Eureka). */
@FeignClient(name = "carrito-service")
public interface CarritoClient {

    @GetMapping("/api/carritos/{usuarioId}")
    CarritoCliente obtenerCarrito(@PathVariable("usuarioId") Long usuarioId);

    @DeleteMapping("/api/carritos/{usuarioId}")
    void vaciarCarrito(@PathVariable("usuarioId") Long usuarioId);
}
