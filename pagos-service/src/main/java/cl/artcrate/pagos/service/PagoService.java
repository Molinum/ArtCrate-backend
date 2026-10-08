package cl.artcrate.pagos.service;

import cl.artcrate.pagos.client.CambiarEstadoCliente;
import cl.artcrate.pagos.client.PedidoClient;
import cl.artcrate.pagos.client.PedidoCliente;
import cl.artcrate.pagos.dto.CrearPagoRequest;
import cl.artcrate.pagos.dto.PagoResponse;
import cl.artcrate.pagos.exception.ConflictoException;
import cl.artcrate.pagos.exception.RecursoNoEncontradoException;
import cl.artcrate.pagos.model.EstadoPago;
import cl.artcrate.pagos.model.Pago;
import cl.artcrate.pagos.repository.PagoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Lógica de negocio de los pagos (simulados). Consulta y actualiza el pedido en pedidos-service. */
@Service
public class PagoService {

    private static final String PENDIENTE_PAGO = "PENDIENTE_PAGO";
    private static final String PAGADO = "PAGADO";

    private final PagoRepository repository;
    private final PedidoClient pedidoClient;

    public PagoService(PagoRepository repository, PedidoClient pedidoClient) {
        this.repository = repository;
        this.pedidoClient = pedidoClient;
    }

    /**
     * Paga un pedido. Pasos:
     *   1) consultar el pedido (si no existe -> 404)
     *   2) comprobar que esté PENDIENTE_PAGO (si no -> 409)
     *   3) guardar el pago como APROBADO (simulado)
     *   4) marcar el pedido como PAGADO
     * Si el paso 4 falla, se borra el pago para no dejar un pago aprobado sobre un pedido sin pagar.
     */
    public PagoResponse pagar(CrearPagoRequest datos) {
        PedidoCliente pedido = pedidoClient.obtenerPedido(datos.pedidoId());

        if (!PENDIENTE_PAGO.equals(pedido.estado())) {
            throw new ConflictoException("El pedido " + pedido.id() + " no está pendiente de pago (estado actual: "
                    + pedido.estado() + ")");
        }

        Pago pago = new Pago();
        pago.setPedidoId(pedido.id());
        pago.setMonto(pedido.total()); // el monto lo define el pedido, no el cliente
        pago.setMetodo(datos.metodo());
        pago.setEstado(EstadoPago.APROBADO); // pago simulado: siempre se aprueba
        pago.setFecha(LocalDateTime.now());
        pago.setCodigoTransaccion(UUID.randomUUID().toString());
        Pago guardado = repository.save(pago);

        try {
            pedidoClient.cambiarEstado(pedido.id(), new CambiarEstadoCliente(PAGADO));
        } catch (RuntimeException e) {
            repository.delete(guardado);
            throw e;
        }
        return aRespuesta(guardado);
    }

    public PagoResponse obtener(Long id) {
        return aRespuesta(repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pago con id " + id)));
    }

    public List<PagoResponse> listarPorPedido(Long pedidoId) {
        return repository.findByPedidoId(pedidoId).stream().map(this::aRespuesta).toList();
    }

    private PagoResponse aRespuesta(Pago p) {
        return new PagoResponse(p.getId(), p.getPedidoId(), p.getMonto(), p.getMetodo(), p.getEstado(),
                p.getFecha(), p.getCodigoTransaccion());
    }
}
