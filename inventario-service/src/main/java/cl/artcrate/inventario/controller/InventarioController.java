package cl.artcrate.inventario.controller;

import cl.artcrate.inventario.dto.ActualizarStockRequest;
import cl.artcrate.inventario.dto.ItemStockRequest;
import cl.artcrate.inventario.dto.StockResponse;
import cl.artcrate.inventario.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST de /api/inventario. */
@RestController
@RequestMapping("/api/inventario")
@Tag(name = "Inventario", description = "Stock disponible de cada producto")
public class InventarioController {

    private final InventarioService service;

    public InventarioController(InventarioService service) {
        this.service = service;
    }

    @Operation(summary = "Listar el stock de todos los productos")
    @GetMapping
    public List<StockResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Obtener el stock de un producto")
    @GetMapping("/{productoId}")
    public StockResponse obtener(@PathVariable Long productoId) {
        return service.obtener(productoId);
    }

    // Fija la cantidad exacta (sirve para el panel de administración)
    @Operation(summary = "Fijar la cantidad en stock de un producto")
    @PutMapping("/{productoId}")
    public StockResponse fijar(@PathVariable Long productoId, @Valid @RequestBody ActualizarStockRequest datos) {
        return service.fijar(productoId, datos.cantidad());
    }

    // Todo o nada: si falta stock de cualquier producto, responde 409 y no descuenta ninguno
    @Operation(summary = "Descontar stock de varios productos (todo o nada)")
    @PostMapping("/descontar")
    public List<StockResponse> descontar(@RequestBody List<ItemStockRequest> items) {
        return service.descontar(items);
    }

    @Operation(summary = "Reponer stock de varios productos")
    @PostMapping("/reponer")
    public List<StockResponse> reponer(@RequestBody List<ItemStockRequest> items) {
        return service.reponer(items);
    }
}
