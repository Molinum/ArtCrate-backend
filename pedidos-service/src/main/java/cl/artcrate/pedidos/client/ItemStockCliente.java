package cl.artcrate.pedidos.client;

/** Producto y cantidad que se envían a inventario-service para descontar o reponer. */
public record ItemStockCliente(
        Long productoId,
        int cantidad) {
}
