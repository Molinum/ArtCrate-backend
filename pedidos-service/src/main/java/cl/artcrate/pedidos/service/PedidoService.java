package cl.artcrate.pedidos.service;

import cl.artcrate.pedidos.client.CarritoClient;
import cl.artcrate.pedidos.client.CarritoCliente;
import cl.artcrate.pedidos.client.InventarioClient;
import cl.artcrate.pedidos.client.ItemCarritoCliente;
import cl.artcrate.pedidos.client.ItemStockCliente;
import cl.artcrate.pedidos.dto.CrearPedidoRequest;
import cl.artcrate.pedidos.dto.ItemPedidoResponse;
import cl.artcrate.pedidos.dto.PedidoResponse;
import cl.artcrate.pedidos.exception.ConflictoException;
import cl.artcrate.pedidos.exception.RecursoNoEncontradoException;
import cl.artcrate.pedidos.exception.SolicitudInvalidaException;
import cl.artcrate.pedidos.model.EstadoPedido;
import cl.artcrate.pedidos.model.ItemPedido;
import cl.artcrate.pedidos.model.Pedido;
import cl.artcrate.pedidos.repository.PedidoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio de los pedidos.
 * Habla con carrito-service (para leer y vaciar el carrito) y con inventario-service (para descontar y reponer stock).
 */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    // Único camino permitido hacia adelante. CANCELADO se trata aparte (solo desde PENDIENTE_PAGO).
    private static final Map<EstadoPedido, EstadoPedido> SIGUIENTE_ESTADO = Map.of(
            EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO,
            EstadoPedido.PAGADO, EstadoPedido.ENVIADO,
            EstadoPedido.ENVIADO, EstadoPedido.ENTREGADO);

    private final PedidoRepository repository;
    private final CarritoClient carritoClient;
    private final InventarioClient inventarioClient;

    public PedidoService(PedidoRepository repository, CarritoClient carritoClient, InventarioClient inventarioClient) {
        this.repository = repository;
        this.carritoClient = carritoClient;
        this.inventarioClient = inventarioClient;
    }

    /**
     * Crea un pedido a partir del carrito del usuario. Pasos:
     *   1) leer el carrito (si está vacío -> 400)
     *   2) descontar el stock, todo o nada (si falta stock -> 409)
     *   3) guardar el pedido en PENDIENTE_PAGO
     *   4) vaciar el carrito
     * Este método NO es @Transactional a propósito: hace llamadas a otros servicios y,
     * si algo falla a mitad de camino, deshacemos a mano lo que ya se hizo (reponer el stock).
     */
    public PedidoResponse crear(CrearPedidoRequest datos) {
        // 1) Leer el carrito
        CarritoCliente carrito = carritoClient.obtenerCarrito(datos.usuarioId());
        if (carrito.items() == null || carrito.items().isEmpty()) {
            throw new SolicitudInvalidaException("El carrito está vacío");
        }

        // 2) Descontar el stock (si falta alguno, inventario responde 409 y no descuenta nada)
        List<ItemStockCliente> aDescontar = carrito.items().stream()
                .map(i -> new ItemStockCliente(i.productoId(), i.cantidad()))
                .toList();
        inventarioClient.descontar(aDescontar);

        // 3) Guardar el pedido. Si falla, se devuelve el stock que ya se había descontado
        Pedido pedido;
        try {
            pedido = guardarPedido(datos, carrito);
        } catch (RuntimeException e) {
            reponerSinFallar(aDescontar);
            throw e;
        }

        // 4) Vaciar el carrito. Si no se puede, se deshace todo para no dejar un pedido a medias
        try {
            carritoClient.vaciarCarrito(datos.usuarioId());
        } catch (RuntimeException e) {
            deshacerPedido(pedido);
            reponerSinFallar(aDescontar);
            throw e;
        }

        return aRespuesta(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return repository.findAll().stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorUsuario(Long usuarioId) {
        return repository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream().map(this::aRespuesta).toList();
    }

    /**
     * Cambia el estado respetando el flujo PENDIENTE_PAGO -> PAGADO -> ENVIADO -> ENTREGADO.
     * CANCELADO solo se permite desde PENDIENTE_PAGO y devuelve el stock al inventario.
     */
    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevo) {
        // buscarParaActualizar bloquea la fila: dos cambios a la vez se hacen uno después del otro
        Pedido pedido = repository.buscarParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido con id " + id));

        EstadoPedido actual = pedido.getEstado();
        boolean permitido = (nuevo == EstadoPedido.CANCELADO)
                ? actual == EstadoPedido.PENDIENTE_PAGO
                : SIGUIENTE_ESTADO.get(actual) == nuevo;
        if (!permitido) {
            throw new ConflictoException("No se puede cambiar el pedido de " + actual + " a " + nuevo);
        }

        if (nuevo == EstadoPedido.CANCELADO) {
            // Primero se repone el stock; si inventario no responde, el pedido NO cambia de estado
            inventarioClient.reponer(pedido.getItems().stream()
                    .map(i -> new ItemStockCliente(i.getProductoId(), i.getCantidad()))
                    .toList());
        }

        pedido.setEstado(nuevo);
        return aRespuesta(repository.save(pedido));
    }

    // ----- métodos de apoyo -----

    private Pedido buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido con id " + id));
    }

    /** Arma el pedido copiando los ítems del carrito y calculando el total. */
    private Pedido guardarPedido(CrearPedidoRequest datos, CarritoCliente carrito) {
        Pedido pedido = new Pedido();
        pedido.setUsuarioId(datos.usuarioId());
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.setDireccionDespacho(datos.direccionDespacho().trim());
        pedido.setComuna(datos.comuna().trim());
        pedido.setRegion(datos.region().trim());

        long total = 0;
        for (ItemCarritoCliente item : carrito.items()) {
            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setPedido(pedido);
            itemPedido.setProductoId(item.productoId());
            itemPedido.setNombre(item.nombre());
            itemPedido.setPrecioUnitario(item.precioUnitario());
            itemPedido.setCantidad(item.cantidad());
            long subtotal = item.precioUnitario() * item.cantidad(); // se recalcula aquí, no se confía en el carrito
            itemPedido.setSubtotal(subtotal);
            pedido.getItems().add(itemPedido);
            total += subtotal;
        }
        pedido.setTotal(total);
        return repository.save(pedido);
    }

    /** Devuelve el stock; si también esto falla, solo queda registrado en el log (ya estamos manejando otro error). */
    private void reponerSinFallar(List<ItemStockCliente> items) {
        try {
            inventarioClient.reponer(items);
        } catch (Exception e) {
            log.error("No se pudo reponer el stock {}: {}", items, e.getMessage());
        }
    }

    private void deshacerPedido(Pedido pedido) {
        try {
            repository.delete(pedido);
        } catch (Exception e) {
            log.error("No se pudo eliminar el pedido {}: {}", pedido.getId(), e.getMessage());
        }
    }

    private PedidoResponse aRespuesta(Pedido p) {
        List<ItemPedidoResponse> items = p.getItems().stream()
                .map(i -> new ItemPedidoResponse(i.getProductoId(), i.getNombre(), i.getPrecioUnitario(),
                        i.getCantidad(), i.getSubtotal()))
                .toList();
        return new PedidoResponse(p.getId(), p.getUsuarioId(), p.getFecha(), p.getEstado(), p.getTotal(),
                p.getDireccionDespacho(), p.getComuna(), p.getRegion(), items);
    }
}
