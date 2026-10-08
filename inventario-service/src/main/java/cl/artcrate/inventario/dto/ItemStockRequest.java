package cl.artcrate.inventario.dto;

/** Un producto con la cantidad a descontar o reponer. */
public record ItemStockRequest(
        Long productoId,
        Integer cantidad) {
}
