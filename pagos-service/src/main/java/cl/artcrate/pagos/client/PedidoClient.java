package cl.artcrate.pagos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign de pedidos-service (se encuentra por su nombre en Eureka).
 * El PATCH funciona gracias a la dependencia feign-hc5 del pom (el cliente HTTP por defecto no lo soporta).
 */
@FeignClient(name = "pedidos-service")
public interface PedidoClient {

    @GetMapping("/api/pedidos/{id}")
    PedidoCliente obtenerPedido(@PathVariable("id") Long id);

    @PatchMapping("/api/pedidos/{id}/estado")
    PedidoCliente cambiarEstado(@PathVariable("id") Long id, @RequestBody CambiarEstadoCliente cuerpo);
}
