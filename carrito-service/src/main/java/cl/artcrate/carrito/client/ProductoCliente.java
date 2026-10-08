package cl.artcrate.carrito.client;

/** Lo que usamos de la respuesta de catalogo-service (DTO propio: no se comparten clases entre servicios). */
public record ProductoCliente(
        Long id,
        String nombre,
        long precio,
        String imagenUrl,
        boolean activo) {
}
