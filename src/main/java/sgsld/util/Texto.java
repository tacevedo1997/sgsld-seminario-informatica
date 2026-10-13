package sgsld.util;

import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilidades de texto que replican en Java la intercalación
 * {@code utf8mb4_0900_ai_ci} de la base de datos: las comparaciones y la
 * unicidad no distinguen mayúsculas ni tildes, y el orden alfabético sigue las
 * reglas del español.
 */
public final class Texto {

    /** Configuración regional del sistema (español de Argentina). */
    public static final Locale LOCALE = Locale.forLanguageTag("es-AR");

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");

    private static final Collator COLLATOR = crearCollator();

    private Texto() {
    }

    /**
     * Normaliza un texto para compararlo: quita espacios en los extremos, quita
     * tildes y diéresis (descomposición NFD) y pasa a minúsculas. Por ejemplo,
     * {@code "  Pérez "} y {@code "PEREZ"} quedan como {@code "perez"}.
     *
     * @param texto texto original (puede ser {@code null})
     * @return texto normalizado; cadena vacía si es {@code null}
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String descompuesto = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD);
        return MARCAS_DIACRITICAS.matcher(descompuesto).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /**
     * Indica si dos textos son iguales sin distinguir mayúsculas ni tildes.
     *
     * @param a primer texto
     * @param b segundo texto
     * @return {@code true} si coinciden una vez normalizados
     */
    public static boolean iguales(String a, String b) {
        return normalizar(a).equals(normalizar(b));
    }

    /**
     * Indica si un texto contiene a otro sin distinguir mayúsculas ni tildes
     * (equivalente a {@code LIKE '%fragmento%'}).
     *
     * @param texto     texto donde se busca
     * @param fragmento texto buscado
     * @return {@code true} si el fragmento aparece en el texto
     */
    public static boolean contiene(String texto, String fragmento) {
        return normalizar(texto).contains(normalizar(fragmento));
    }

    /**
     * Indica si un texto es nulo o solo contiene espacios.
     *
     * @param texto texto a evaluar
     * @return {@code true} si no tiene contenido
     */
    public static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    /**
     * Compara dos textos en orden alfabético español con fuerza
     * {@code PRIMARY} (ignora mayúsculas y tildes).
     *
     * @param a primer texto
     * @param b segundo texto
     * @return negativo, cero o positivo, como {@link Comparator#compare}
     */
    public static int comparar(String a, String b) {
        return COLLATOR.compare(a, b);
    }

    /**
     * Devuelve el criterio de orden alfabético español.
     *
     * @return comparador basado en {@link #comparar(String, String)}
     */
    public static Comparator<String> ordenAlfabetico() {
        return Texto::comparar;
    }

    private static Collator crearCollator() {
        Collator collator = Collator.getInstance(LOCALE);
        collator.setStrength(Collator.PRIMARY);
        return collator;
    }
}
