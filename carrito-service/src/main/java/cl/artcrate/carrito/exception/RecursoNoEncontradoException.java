package cl.artcrate.carrito.exception;

/** Se lanza cuando no existe lo que se busca (responde 404). */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
