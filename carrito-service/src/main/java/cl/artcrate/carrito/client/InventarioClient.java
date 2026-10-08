package cl.artcrate.carrito.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Cliente Feign de inventario-service (servicios.inventario.url). */
@FeignClient(name = "inventario-service", url = "${servicios.inventario.url}")
public interface InventarioClient {

    @GetMapping("/api/inventario/{productoId}")
    StockCliente obtenerStock(@PathVariable("productoId") Long productoId);
}
