package sgsld.util;

/**
 * Indica que no hay sesión iniciada o que el rol del usuario no permite la
 * operación solicitada (control de acceso por rol, RNF07).
 */
public class AccesoDenegadoException extends SgsldException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje para el usuario.
     *
     * @param mensaje motivo de la denegación
     */
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
