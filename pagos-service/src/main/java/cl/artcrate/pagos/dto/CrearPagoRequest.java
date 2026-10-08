package cl.artcrate.pagos.dto;

import cl.artcrate.pagos.model.MetodoPago;
import jakarta.validation.constraints.NotNull;

public record CrearPagoRequest(
        @NotNull(message = "El pedidoId es obligatorio")
        Long pedidoId,

        @NotNull(message = "El método de pago es obligatorio (TARJETA o TRANSFERENCIA)")
        MetodoPago metodo) {
}
