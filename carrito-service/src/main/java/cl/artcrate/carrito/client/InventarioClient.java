package cl.artcrate.carrito.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Cliente Feign de inventario-service (se encuentra por su nombre en Eureka). */
@FeignClient(name = "inventario-service")
public interface InventarioClient {

    @GetMapping("/api/inventario/{productoId}")
    StockCliente obtenerStock(@PathVariable("productoId") Long productoId);
}
