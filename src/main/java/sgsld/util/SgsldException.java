package sgsld.util;

/**
 * Raíz abstracta de las excepciones comprobadas del SGSLD.
 * <p>
 * Agrupa los errores esperables del dominio (validación, entidad inexistente,
 * acceso denegado, credenciales inválidas), que el compilador obliga a declarar
 * o capturar. {@code Menu.mostrar()} la captura como red de seguridad.
 */
public abstract class SgsldException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje para el usuario.
     *
     * @param mensaje descripción del problema
     */
    protected SgsldException(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea la excepción con un mensaje y la causa original encadenada.
     *
     * @param mensaje descripción del problema
     * @param causa   excepción que la originó
     */
    protected SgsldException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
