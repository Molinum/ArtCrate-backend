package cl.artcrate.carrito.client;

/** Lo que usamos de la respuesta de inventario-service. */
public record StockCliente(
        Long productoId,
        int cantidad) {
}
