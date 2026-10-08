package cl.artcrate.pedidos.client;

import java.util.List;

/** El carrito tal como lo devuelve carrito-service. */
public record CarritoCliente(
        Long id,
        Long usuarioId,
        List<ItemCarritoCliente> items,
        long total) {
}
