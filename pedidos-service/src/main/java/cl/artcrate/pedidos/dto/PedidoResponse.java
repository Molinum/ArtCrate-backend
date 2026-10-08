package cl.artcrate.pedidos.dto;

import cl.artcrate.pedidos.model.EstadoPedido;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        Long usuarioId,
        LocalDateTime fecha,
        EstadoPedido estado,
        long total,
        String direccionDespacho,
        String comuna,
        String region,
        List<ItemPedidoResponse> items) {
}
