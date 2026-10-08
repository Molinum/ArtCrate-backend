package cl.artcrate.carrito.dto;

import java.util.List;

/** El carrito con sus ítems y el total, todo calculado en el backend. */
public record CarritoResponse(
        Long id,
        Long usuarioId,
        List<ItemCarritoResponse> items,
        long total) {
}
