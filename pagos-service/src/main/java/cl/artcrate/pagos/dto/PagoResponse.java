package cl.artcrate.pagos.dto;

import cl.artcrate.pagos.model.EstadoPago;
import cl.artcrate.pagos.model.MetodoPago;
import java.time.LocalDateTime;

public record PagoResponse(
        Long id,
        Long pedidoId,
        long monto,
        MetodoPago metodo,
        EstadoPago estado,
        LocalDateTime fecha,
        String codigoTransaccion) {
}
