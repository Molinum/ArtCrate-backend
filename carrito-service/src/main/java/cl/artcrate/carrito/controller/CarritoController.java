package cl.artcrate.carrito.controller;

import cl.artcrate.carrito.dto.AgregarItemRequest;
import cl.artcrate.carrito.dto.CambiarCantidadRequest;
import cl.artcrate.carrito.dto.CarritoResponse;
import cl.artcrate.carrito.service.CarritoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST de /api/carritos/{usuarioId}. */
@RestController
@RequestMapping("/api/carritos/{usuarioId}")
public class CarritoController {

    private final CarritoService service;

    public CarritoController(CarritoService service) {
        this.service = service;
    }

    @GetMapping
    public CarritoResponse obtener(@PathVariable Long usuarioId) {
        return service.obtener(usuarioId);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public CarritoResponse agregar(@PathVariable Long usuarioId, @Valid @RequestBody AgregarItemRequest datos) {
        return service.agregar(usuarioId, datos);
    }

    @PutMapping("/items/{productoId}")
    public CarritoResponse cambiarCantidad(@PathVariable Long usuarioId, @PathVariable Long productoId,
                                           @Valid @RequestBody CambiarCantidadRequest datos) {
        return service.cambiarCantidad(usuarioId, productoId, datos.cantidad());
    }

    @DeleteMapping("/items/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void quitarItem(@PathVariable Long usuarioId, @PathVariable Long productoId) {
        service.quitarItem(usuarioId, productoId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void vaciar(@PathVariable Long usuarioId) {
        service.vaciar(usuarioId);
    }
}
