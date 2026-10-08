package cl.artcrate.inventario.service;

import cl.artcrate.inventario.dto.ItemStockRequest;
import cl.artcrate.inventario.dto.StockResponse;
import cl.artcrate.inventario.exception.ConflictoException;
import cl.artcrate.inventario.exception.RecursoNoEncontradoException;
import cl.artcrate.inventario.exception.SolicitudInvalidaException;
import cl.artcrate.inventario.model.Stock;
import cl.artcrate.inventario.repository.StockRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lógica de negocio del inventario. */
@Service
public class InventarioService {

    private final StockRepository repository;

    public InventarioService(StockRepository repository) {
        this.repository = repository;
    }

    public List<StockResponse> listar() {
        return repository.findAll(Sort.by("productoId")).stream().map(this::aRespuesta).toList();
    }

    public StockResponse obtener(Long productoId) {
        Stock stock = repository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El producto " + productoId + " no tiene stock registrado"));
        return aRespuesta(stock);
    }

    /** Fija la cantidad exacta de un producto. Si todavía no tenía stock registrado, lo crea. */
    public StockResponse fijar(Long productoId, int cantidad) {
        Stock stock = repository.findById(productoId).orElse(new Stock(productoId, 0));
        stock.setCantidad(cantidad);
        return aRespuesta(repository.save(stock));
    }

    /**
     * Descuenta stock de varios productos: TODO o NADA.
     * Primero se revisa que alcance para todos; solo si alcanza se descuenta.
     * Si algo falla, la transacción se deshace y ningún producto cambia.
     */
    @Transactional
    public List<StockResponse> descontar(List<ItemStockRequest> items) {
        Map<Long, Integer> pedido = agrupar(items);
        Map<Long, Stock> stocks = cargarBloqueados(pedido);

        // 1) Revisar TODOS los productos antes de tocar nada
        List<String> problemas = new ArrayList<>();
        pedido.forEach((productoId, cantidad) -> {
            Stock stock = stocks.get(productoId);
            if (stock == null) {
                problemas.add("El producto " + productoId + " no tiene stock registrado");
            } else if (stock.getCantidad() < cantidad) {
                problemas.add("Stock insuficiente para el producto " + productoId
                        + " (disponible: " + stock.getCantidad() + ", solicitado: " + cantidad + ")");
            }
        });
        if (!problemas.isEmpty()) {
            throw new ConflictoException(String.join("; ", problemas));
        }

        // 2) Si todo alcanza, recién ahora se descuenta
        pedido.forEach((productoId, cantidad) -> {
            Stock stock = stocks.get(productoId);
            stock.setCantidad(stock.getCantidad() - cantidad);
        });
        return repository.saveAll(stocks.values()).stream().map(this::aRespuesta).toList();
    }

    /** Devuelve stock (por una cancelación o un error). Si el producto no tenía registro, lo crea. */
    @Transactional
    public List<StockResponse> reponer(List<ItemStockRequest> items) {
        Map<Long, Integer> pedido = agrupar(items);
        Map<Long, Stock> stocks = cargarBloqueados(pedido);

        pedido.forEach((productoId, cantidad) -> {
            Stock stock = stocks.computeIfAbsent(productoId, id -> new Stock(id, 0));
            stock.setCantidad(stock.getCantidad() + cantidad);
        });
        return repository.saveAll(stocks.values()).stream().map(this::aRespuesta).toList();
    }

    // ----- métodos de apoyo -----

    /**
     * Valida la lista y junta los productos repetidos sumando sus cantidades
     * (por ejemplo, dos líneas del producto 3 con 1 y 2 unidades pasan a ser 3 unidades).
     */
    private Map<Long, Integer> agrupar(List<ItemStockRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new SolicitudInvalidaException("La lista de productos no puede estar vacía");
        }
        Map<Long, Integer> agrupado = new LinkedHashMap<>();
        for (ItemStockRequest item : items) {
            if (item == null || item.productoId() == null || item.cantidad() == null || item.cantidad() < 1) {
                throw new SolicitudInvalidaException(
                        "Cada producto necesita un productoId y una cantidad mayor a 0");
            }
            agrupado.merge(item.productoId(), item.cantidad(), Integer::sum);
        }
        return agrupado;
    }

    /** Carga (y bloquea) los stocks de los productos pedidos, indexados por productoId. */
    private Map<Long, Stock> cargarBloqueados(Map<Long, Integer> pedido) {
        return repository.buscarYBloquear(pedido.keySet()).stream()
                .collect(Collectors.toMap(Stock::getProductoId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private StockResponse aRespuesta(Stock s) {
        return new StockResponse(s.getProductoId(), s.getCantidad());
    }
}
