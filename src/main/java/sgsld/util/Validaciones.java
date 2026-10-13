package sgsld.util;

import java.util.List;

/**
 * Comprobaciones reutilizables que acumulan mensajes de error en una lista,
 * para informar todos los problemas de un formulario de una sola vez.
 * <p>
 * Las longitudes se cuentan en caracteres Unicode (puntos de código), igual
 * que {@code VARCHAR(n)} en MySQL con {@code utf8mb4}.
 */
public final class Validaciones {

    private Validaciones() {
    }

    /**
     * Controla un dato de texto: obligatoriedad y longitud máxima.
     *
     * @param errores     lista donde se agregan los errores
     * @param valor       texto a controlar
     * @param campo       nombre del campo para el mensaje (en minúsculas)
     * @param obligatorio si el campo no puede quedar vacío
     * @param maxLongitud cantidad máxima de caracteres
     */
    public static void texto(List<String> errores, String valor, String campo,
                             boolean obligatorio, int maxLongitud) {
        if (Texto.estaVacio(valor)) {
            if (obligatorio) {
                errores.add("El campo " + campo + " es obligatorio.");
            }
            return;
        }
        if (valor.codePointCount(0, valor.length()) > maxLongitud) {
            errores.add("El campo " + campo + " admite hasta " + maxLongitud + " caracteres.");
        }
    }

    /**
     * Controla que un dato no textual (fecha, selección de una lista) esté presente.
     *
     * @param errores lista donde se agregan los errores
     * @param valor   dato a controlar
     * @param campo   nombre del campo para el mensaje (en minúsculas)
     */
    public static void obligatorio(List<String> errores, Object valor, String campo) {
        if (valor == null) {
            errores.add("El campo " + campo + " es obligatorio.");
        }
    }
}
