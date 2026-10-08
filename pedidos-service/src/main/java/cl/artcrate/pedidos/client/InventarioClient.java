package cl.artcrate.pedidos.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Cliente Feign de inventario-service (se encuentra por su nombre en Eureka). */
@FeignClient(name = "inventario-service")
public interface InventarioClient {

    // Todo o nada: si falta stock de cualquier producto, inventario responde 409 y no descuenta nada
    @PostMapping("/api/inventario/descontar")
    void descontar(@RequestBody List<ItemStockCliente> items);

    @PostMapping("/api/inventario/reponer")
    void reponer(@RequestBody List<ItemStockCliente> items);
}
