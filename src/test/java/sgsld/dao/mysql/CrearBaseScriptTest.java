package sgsld.dao.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@code sql/crear_base.sql} es una copia de las secciones 1 a 3 de
 * {@code sgsld_script.sql}: esta prueba (sin MySQL) evita que las dos copias
 * del lote de prueba difieran.
 */
@DisplayName("sql/crear_base.sql · copia fiel de las secciones 1 a 3 de sgsld_script.sql")
class CrearBaseScriptTest {

    @Test
    @DisplayName("El bloque de crear_base.sql es idéntico a las secciones 1 a 3 del script")
    void bloqueIdentico() throws Exception {
        assertEquals(ScriptBase.seccionesUnoATres(ScriptBase.leer(ScriptBase.SCRIPT)),
                ScriptBase.seccionesUnoATres(ScriptBase.leer(ScriptBase.CREAR_BASE)));
    }

    @Test
    @DisplayName("Antes del bloque, crear_base.sql solo tiene comentarios")
    void encabezadoSoloComentarios() throws Exception {
        for (String linea : ScriptBase.encabezado(ScriptBase.leer(ScriptBase.CREAR_BASE))) {
            assertTrue(linea.isBlank() || linea.startsWith("--"), "Línea inesperada: " + linea);
        }
    }

    @Test
    @DisplayName("crear_base.sql no incluye consultas, borrados ni la limpieza del lote (secciones 4 a 8)")
    void sinSeccionesPosteriores() throws Exception {
        String texto = ScriptBase.leer(ScriptBase.CREAR_BASE);
        assertFalse(texto.contains("-- 4. CONSULTAS"));
        assertFalse(texto.contains("DELETE FROM"));
        assertFalse(texto.contains("UPDATE jugador"));
        assertTrue(texto.contains("INSERT INTO evolucion"));
    }
}
