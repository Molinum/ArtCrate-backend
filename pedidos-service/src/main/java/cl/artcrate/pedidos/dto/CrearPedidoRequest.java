package cl.artcrate.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearPedidoRequest(
        @NotNull(message = "El usuarioId es obligatorio")
        Long usuarioId,

        @NotBlank(message = "La dirección de despacho es obligatoria")
        String direccionDespacho,

        @NotBlank(message = "La comuna es obligatoria")
        String comuna,

        @NotBlank(message = "La región es obligatoria")
        String region) {
}
