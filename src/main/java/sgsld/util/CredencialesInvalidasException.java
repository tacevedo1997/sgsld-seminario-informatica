package sgsld.util;

/**
 * Indica que el inicio de sesión falló.
 * <p>
 * El mensaje es siempre genérico: no revela si el error está en el usuario o
 * en la contraseña (CU-09, flujo alternativo 2a).
 */
public class CredencialesInvalidasException extends SgsldException {

    private static final long serialVersionUID = 1L;

    /** Mensaje único para cualquier falla de autenticación. */
    public static final String MENSAJE = "Usuario o contraseña incorrectos";

    /** Crea la excepción con el mensaje genérico. */
    public CredencialesInvalidasException() {
        super(MENSAJE);
    }
}
