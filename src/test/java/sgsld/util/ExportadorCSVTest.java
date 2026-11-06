package sgsld.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exportación a CSV para Excel en español (UTF-8 con BOM, punto y coma, CRLF). */
@DisplayName("ExportadorCSV · formato para Excel en español")
class ExportadorCSVTest {

    @TempDir
    Path directorio;

    @Test
    @DisplayName("BOM UTF-8, separador ; y fin de línea CRLF; tildes intactas")
    void formato() throws Exception {
        Path archivo = directorio.resolve("reporte.csv");
        ExportadorCSV.exportar(new String[][] {{"Zona corporal", "Lesiones"}, {"Cuádriceps", "1"}, {"Total", "8"}},
                archivo);
        byte[] bytes = Files.readAllBytes(archivo);
        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, Arrays.copyOf(bytes, 3));
        String texto = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertEquals("Zona corporal;Lesiones\r\nCuádriceps;1\r\nTotal;8\r\n", texto);
    }

    @Test
    @DisplayName("Valores con ; comillas o saltos de línea van entre comillas (comillas duplicadas); null vacío")
    void escapado() {
        assertEquals("31,5", ExportadorCSV.escapar("31,5"));
        assertEquals("\"Fisioterapia; readaptación\"", ExportadorCSV.escapar("Fisioterapia; readaptación"));
        assertEquals("\"Dijo \"\"alta\"\"\"", ExportadorCSV.escapar("Dijo \"alta\""));
        assertEquals("\"dos\nrenglones\"", ExportadorCSV.escapar("dos\nrenglones"));
        assertEquals("", ExportadorCSV.escapar(null));
    }

    @Test
    @DisplayName("Nombre sugerido: reporte-<criterio>-<desde>-<hasta>.csv, sin tildes ni espacios")
    void nombreSugerido() {
        assertEquals("reporte-lesiones-por-zona-corporal-2026-01-01-2026-09-26.csv", ExportadorCSV.nombreSugerido(
                "Lesiones por zona corporal", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 26)));
        assertEquals("reporte-tratamientos-en-curso-y-finalizados-2026-09-26.csv", ExportadorCSV.nombreSugerido(
                "Tratamientos en curso y finalizados", LocalDate.of(2026, 9, 26), null));
        assertEquals("reporte-duracion-promedio-de-tratamientos-finalizados-por-tipo-de-lesion-2026-01-01-2026-09-26.csv",
                ExportadorCSV.nombreSugerido("Duración promedio de tratamientos finalizados por tipo de lesión",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 26)));
    }
}
