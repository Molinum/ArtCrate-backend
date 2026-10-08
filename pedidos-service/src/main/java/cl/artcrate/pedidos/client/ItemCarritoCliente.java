package cl.artcrate.pedidos.client;

/** Un ítem tal como lo devuelve carrito-service (DTO propio de este servicio). */
public record ItemCarritoCliente(
        Long productoId,
        String nombre,
        long precioUnitario,
        int cantidad,
        long subtotal) {
}
