package cl.artcrate.pagos.exception;

/** Se lanza cuando la operación choca con el estado actual, por ejemplo un correo repetido (responde 409). */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
