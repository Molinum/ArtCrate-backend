package cl.artcrate.pagos.client;

/** Lo que usamos de la respuesta de pedidos-service (el estado llega como texto, por ejemplo "PENDIENTE_PAGO"). */
public record PedidoCliente(
        Long id,
        Long usuarioId,
        String estado,
        long total) {
}
