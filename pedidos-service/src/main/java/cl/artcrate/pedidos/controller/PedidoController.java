package cl.artcrate.pedidos.controller;

import cl.artcrate.pedidos.dto.CambiarEstadoRequest;
import cl.artcrate.pedidos.dto.CrearPedidoRequest;
import cl.artcrate.pedidos.dto.PedidoResponse;
import cl.artcrate.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST de /api/pedidos. */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public PedidoResponse crear(@Valid @RequestBody CrearPedidoRequest datos) {
        return service.crear(datos);
    }

    @GetMapping
    public List<PedidoResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<PedidoResponse> listarPorUsuario(@PathVariable Long usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }

    @PatchMapping("/{id}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest datos) {
        return service.cambiarEstado(id, datos.estado());
    }
}
