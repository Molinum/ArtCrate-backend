package cl.artcrate.pedidos.model;

/** Estados posibles de un pedido. Camino normal: PENDIENTE_PAGO -> PAGADO -> ENVIADO -> ENTREGADO. */
public enum EstadoPedido {
    PENDIENTE_PAGO,
    PAGADO,
    ENVIADO,
    ENTREGADO,
    CANCELADO
}
