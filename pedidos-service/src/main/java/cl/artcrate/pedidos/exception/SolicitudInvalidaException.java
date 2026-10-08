package cl.artcrate.pedidos.exception;

/** Se lanza cuando la petición no se puede procesar, por ejemplo con el carrito vacío (responde 400). */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
