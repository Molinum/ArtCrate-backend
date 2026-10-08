package cl.artcrate.pagos.client;

/** Cuerpo del PATCH que cambia el estado de un pedido. */
public record CambiarEstadoCliente(
        String estado) {
}
