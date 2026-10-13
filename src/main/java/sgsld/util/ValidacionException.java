package sgsld.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Indica que los datos ingresados no cumplen las reglas de negocio.
 * <p>
 * Reúne <b>todos</b> los errores detectados en una validación, para que la vista
 * los muestre juntos y el usuario pueda corregirlos de una vez.
 */
public class ValidacionException extends SgsldException {

    private static final long serialVersionUID = 1L;

    private final ArrayList<String> errores;

    /**
     * Crea la excepción con la lista de errores detectados.
     *
     * @param errores mensajes de error (al menos uno)
     */
    public ValidacionException(List<String> errores) {
        super(componerMensaje(errores));
        this.errores = new ArrayList<>(errores);
    }

    /**
     * Crea la excepción con un único error.
     *
     * @param error mensaje de error
     */
    public ValidacionException(String error) {
        this(List.of(error));
    }

    /**
     * Lanza una {@code ValidacionException} si la lista contiene errores.
     *
     * @param errores errores acumulados por una validación
     * @throws ValidacionException si la lista no está vacía
     */
    public static void lanzarSiHayErrores(List<String> errores) throws ValidacionException {
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
    }

    /**
     * Devuelve los errores detectados.
     *
     * @return lista no modificable de mensajes
     */
    public List<String> getErrores() {
        return Collections.unmodifiableList(errores);
    }

    private static String componerMensaje(List<String> errores) {
        if (errores.isEmpty()) {
            throw new IllegalArgumentException("Una ValidacionException requiere al menos un error");
        }
        if (errores.size() == 1) {
            return errores.get(0);
        }
        return "Se encontraron " + errores.size() + " errores: " + String.join(" ", errores);
    }
}
