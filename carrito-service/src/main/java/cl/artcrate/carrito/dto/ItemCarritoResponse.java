package cl.artcrate.carrito.dto;

public record ItemCarritoResponse(
        Long productoId,
        String nombre,
        long precioUnitario,
        int cantidad,
        String imagenUrl,
        long subtotal) {
}
