package sgsld.util;

/**
 * Indica que no existe el registro buscado (por ejemplo, un jugador o un
 * tratamiento con el número indicado).
 */
public class EntidadNoEncontradaException extends SgsldException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje para el usuario.
     *
     * @param mensaje descripción de lo que no se encontró
     */
    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
