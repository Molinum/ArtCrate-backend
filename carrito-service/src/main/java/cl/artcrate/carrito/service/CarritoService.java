package cl.artcrate.carrito.service;

import cl.artcrate.carrito.client.CatalogoClient;
import cl.artcrate.carrito.client.InventarioClient;
import cl.artcrate.carrito.client.ProductoCliente;
import cl.artcrate.carrito.client.StockCliente;
import cl.artcrate.carrito.dto.AgregarItemRequest;
import cl.artcrate.carrito.dto.CarritoResponse;
import cl.artcrate.carrito.dto.ItemCarritoResponse;
import cl.artcrate.carrito.exception.ConflictoException;
import cl.artcrate.carrito.exception.RecursoNoEncontradoException;
import cl.artcrate.carrito.model.Carrito;
import cl.artcrate.carrito.model.ItemCarrito;
import cl.artcrate.carrito.repository.CarritoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de negocio del carrito.
 * Consulta a catalogo-service (nombre, precio, si está activo) y a inventario-service (stock).
 * El total y los subtotales siempre se calculan aquí, nunca los manda el cliente.
 */
@Service
public class CarritoService {

    private final CarritoRepository repository;
    private final CatalogoClient catalogoClient;
    private final InventarioClient inventarioClient;

    public CarritoService(CarritoRepository repository, CatalogoClient catalogoClient,
            InventarioClient inventarioClient) {
        this.repository = repository;
        this.catalogoClient = catalogoClient;
        this.inventarioClient = inventarioClient;
    }

    /** Devuelve el carrito del usuario; si todavía no tiene uno, lo crea vacío. */
    @Transactional
    public CarritoResponse obtener(Long usuarioId) {
        return aRespuesta(obtenerOCrear(usuarioId));
    }

    @Transactional
    public CarritoResponse agregar(Long usuarioId, AgregarItemRequest datos) {
        Long productoId = datos.productoId();
        int cantidad = datos.cantidad();

        // 1) El producto debe existir (si no, catalogo-service responde 404) y estar activo
        ProductoCliente producto = catalogoClient.obtenerProducto(productoId);
        if (!producto.activo()) {
            throw new RecursoNoEncontradoException("El producto " + productoId + " no está disponible");
        }

        // 2) Nunca se puede superar el stock
        StockCliente stock = inventarioClient.obtenerStock(productoId);
        Carrito carrito = obtenerOCrear(usuarioId);
        ItemCarrito item = buscarItem(carrito, productoId);
        int enCarrito = (item == null) ? 0 : item.getCantidad();
        int nuevaCantidad = enCarrito + cantidad; // si ya estaba, las cantidades se suman
        if (nuevaCantidad > stock.cantidad()) {
            throw new ConflictoException("No hay stock suficiente de " + producto.nombre()
                    + ". Disponible: " + stock.cantidad() + ", ya tienes " + enCarrito + " en el carrito");
        }

        // 3) Se crea el ítem copiando los datos del catálogo (o se actualiza si ya estaba)
        if (item == null) {
            item = new ItemCarrito();
            item.setCarrito(carrito);
            item.setProductoId(productoId);
            carrito.getItems().add(item);
        }
        item.setNombre(producto.nombre());
        item.setPrecioUnitario(producto.precio());
        item.setImagenUrl(producto.imagenUrl());
        item.setCantidad(nuevaCantidad);

        return aRespuesta(repository.save(carrito));
    }

    @Transactional
    public CarritoResponse cambiarCantidad(Long usuarioId, Long productoId, int cantidad) {
        Carrito carrito = buscarCarrito(usuarioId);
        ItemCarrito item = buscarItem(carrito, productoId);
        if (item == null) {
            throw new RecursoNoEncontradoException("El producto " + productoId + " no está en el carrito");
        }

        StockCliente stock = inventarioClient.obtenerStock(productoId);
        if (cantidad > stock.cantidad()) {
            throw new ConflictoException("No hay stock suficiente de " + item.getNombre()
                    + ". Disponible: " + stock.cantidad());
        }

        item.setCantidad(cantidad);
        return aRespuesta(repository.save(carrito));
    }

    @Transactional
    public void quitarItem(Long usuarioId, Long productoId) {
        Carrito carrito = buscarCarrito(usuarioId);
        // removeIf saca el ítem de la lista y orphanRemoval lo borra de la base de datos
        boolean quitado = carrito.getItems().removeIf(i -> i.getProductoId().equals(productoId));
        if (!quitado) {
            throw new RecursoNoEncontradoException("El producto " + productoId + " no está en el carrito");
        }
        repository.save(carrito);
    }

    /** Vacía el carrito. Si el usuario no tenía carrito no pasa nada (el resultado es el mismo: vacío). */
    @Transactional
    public void vaciar(Long usuarioId) {
        repository.findByUsuarioId(usuarioId).ifPresent(carrito -> {
            carrito.getItems().clear();
            repository.save(carrito);
        });
    }

    // ----- métodos de apoyo -----

    private Carrito obtenerOCrear(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId).orElseGet(() -> {
            Carrito nuevo = new Carrito();
            nuevo.setUsuarioId(usuarioId);
            return repository.save(nuevo);
        });
    }

    private Carrito buscarCarrito(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El usuario " + usuarioId + " todavía no tiene un carrito"));
    }

    /** Busca un producto dentro del carrito; devuelve null si no está. */
    private ItemCarrito buscarItem(Carrito carrito, Long productoId) {
        return carrito.getItems().stream()
                .filter(i -> i.getProductoId().equals(productoId))
                .findFirst()
                .orElse(null);
    }

    /** Arma la respuesta calculando los subtotales y el total. */
    private CarritoResponse aRespuesta(Carrito carrito) {
        List<ItemCarritoResponse> items = carrito.getItems().stream()
                .map(i -> new ItemCarritoResponse(i.getProductoId(), i.getNombre(), i.getPrecioUnitario(),
                        i.getCantidad(), i.getImagenUrl(), i.subtotal()))
                .toList();
        long total = items.stream().mapToLong(ItemCarritoResponse::subtotal).sum(); // total = suma de subtotales
        return new CarritoResponse(carrito.getId(), carrito.getUsuarioId(), items, total);
    }
}
