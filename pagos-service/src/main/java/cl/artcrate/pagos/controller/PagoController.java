package cl.artcrate.pagos.controller;

import cl.artcrate.pagos.dto.CrearPagoRequest;
import cl.artcrate.pagos.dto.PagoResponse;
import cl.artcrate.pagos.service.PagoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints REST de /api/pagos. */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public PagoResponse pagar(@Valid @RequestBody CrearPagoRequest datos) {
        return service.pagar(datos);
    }

    @GetMapping("/{id}")
    public PagoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/pedido/{pedidoId}")
    public List<PagoResponse> listarPorPedido(@PathVariable Long pedidoId) {
        return service.listarPorPedido(pedidoId);
    }
}
