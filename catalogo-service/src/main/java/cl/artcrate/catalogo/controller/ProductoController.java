package cl.artcrate.catalogo.controller;

import cl.artcrate.catalogo.dto.ProductoRequest;
import cl.artcrate.catalogo.dto.ProductoResponse;
import cl.artcrate.catalogo.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST de /api/productos. */
@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Productos del catálogo")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    // Ejemplos: /api/productos  ·  /api/productos?categoriaId=1  ·  /api/productos?q=acrilico
    @Operation(summary = "Listar productos (con filtro opcional por categoría o búsqueda)")
    @GetMapping
    public List<ProductoResponse> listar(@RequestParam(required = false) Long categoriaId,
                                         @RequestParam(required = false) String q) {
        return service.listar(categoriaId, q);
    }

    @Operation(summary = "Obtener un producto por id")
    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear un producto nuevo")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest datos) {
        return service.crear(datos);
    }

    @Operation(summary = "Actualizar un producto")
    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest datos) {
        return service.actualizar(id, datos);
    }

    @Operation(summary = "Eliminar un producto")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
