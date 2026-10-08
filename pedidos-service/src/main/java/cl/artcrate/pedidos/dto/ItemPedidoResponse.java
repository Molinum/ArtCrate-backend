package cl.artcrate.pedidos.dto;

public record ItemPedidoResponse(
        Long productoId,
        String nombre,
        long precioUnitario,
        int cantidad,
        long subtotal) {
}
