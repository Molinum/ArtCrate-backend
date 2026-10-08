package cl.artcrate.pedidos.dto;

import cl.artcrate.pedidos.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado) {
}
