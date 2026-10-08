package cl.artcrate.inventario.exception;

/** Se lanza cuando la petición viene mal armada, por ejemplo una lista vacía (responde 400). */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
