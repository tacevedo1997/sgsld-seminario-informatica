package sgsld.dao.mysql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Lectura de los scripts SQL del repositorio para las pruebas: extrae las
 * secciones 1 a 3 de {@code sgsld_script.sql} (creación de la base, tablas,
 * vista y lote de prueba), que son las que se ejecutan para crear una base.
 */
final class ScriptBase {

    /** Script de la entrega 2, fuente de verdad del esquema y del lote. */
    static final Path SCRIPT = Path.of("sgsld_script.sql");
    /** Copia de las secciones 1 a 3 para crear o reiniciar la base. */
    static final Path CREAR_BASE = Path.of("sql", "crear_base.sql");

    private ScriptBase() {
    }

    /**
     * Lee un script en UTF-8 con finales de línea normalizados a {@code \n}.
     *
     * @param archivo ruta del script
     * @return el texto
     * @throws IOException si no se puede leer
     */
    static String leer(Path archivo) throws IOException {
        return Files.readString(archivo, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    /**
     * Devuelve el bloque de las secciones 1 a 3: desde el separador que precede
     * a "-- 1." hasta el separador que precede a "-- 4." (o el final del texto).
     *
     * @param texto contenido del script
     * @return el bloque, sin espacios finales
     */
    static String seccionesUnoATres(String texto) {
        List<String> lineas = lineas(texto);
        int cuatro = posicion(lineas, "-- 4. ");
        int fin = cuatro < 0 ? lineas.size() : cuatro - 1;
        return String.join("\n", lineas.subList(inicioDelBloque(lineas), fin)).stripTrailing();
    }

    /**
     * Devuelve las líneas anteriores al bloque de las secciones 1 a 3.
     *
     * @param texto contenido del script
     * @return las líneas del encabezado
     */
    static List<String> encabezado(String texto) {
        List<String> lineas = lineas(texto);
        return lineas.subList(0, inicioDelBloque(lineas));
    }

    private static int inicioDelBloque(List<String> lineas) {
        int uno = posicion(lineas, "-- 1. ");
        if (uno < 1) {
            throw new IllegalStateException("El script no tiene la sección 1");
        }
        return uno - 1;
    }

    private static List<String> lineas(String texto) {
        return Arrays.asList(texto.replace("\r\n", "\n").split("\n", -1));
    }

    private static int posicion(List<String> lineas, String prefijo) {
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).startsWith(prefijo)) {
                return i;
            }
        }
        return -1;
    }
}
