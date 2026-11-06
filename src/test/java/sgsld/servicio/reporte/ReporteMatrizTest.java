package sgsld.servicio.reporte;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;

/** Matriz de exportación de los reportes ({@code String[][]}: encabezados, filas y total). */
@DisplayName("Reportes · matriz de exportación (aMatriz)")
class ReporteMatrizTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 9, 26);

    private final EscenarioPrueba escenario = new EscenarioPrueba().comoCoordinador();

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    private String[][] matriz(Class<? extends Reporte> clase, LocalDate desde, LocalDate hasta) throws Exception {
        for (Reporte reporte : escenario.reportes().getReportesDisponibles()) {
            if (reporte.getClass() == clase) {
                return reporte.aMatriz(escenario.reportes().generar(reporte, desde, hasta));
            }
        }
        throw new AssertionError("No está disponible " + clase.getSimpleName());
    }

    @Test
    @DisplayName("Por zona (Figura 13): encabezados, seis filas y Total 8")
    void conteoConTotal() throws Exception {
        String[][] matriz = matriz(ReportePorZona.class, DESDE, HASTA);
        assertEquals(8, matriz.length);
        assertArrayEquals(new String[] {"Zona corporal", "Lesiones"}, matriz[0]);
        assertArrayEquals(new String[] {"Isquiotibiales", "3"}, matriz[1]);
        assertArrayEquals(new String[] {"Tobillo", "1"}, matriz[6]);
        assertArrayEquals(new String[] {"Total", "8"}, matriz[7]);
    }

    @Test
    @DisplayName("Duración promedio: tres columnas, promedio con coma decimal y sin fila Total")
    void promedioSinTotal() throws Exception {
        String[][] matriz = matriz(ReporteDuracionPromedio.class, DESDE, HASTA);
        assertEquals(List.of("Tipo de lesión", "Tratamientos finalizados", "Duración promedio (días)"),
                List.of(matriz[0]));
        assertArrayEquals(new String[] {"Muscular", "4", "31,5"}, matriz[1]);
        assertArrayEquals(new String[] {"Contusión", "2", "12,0"}, matriz[2]);
        assertEquals(3, matriz.length);
    }

    @Test
    @DisplayName("Tratamientos por estado: En curso 2 · Finalizado 8 · Total 10")
    void estadoConTotal() throws Exception {
        String[][] matriz = matriz(ReporteTratamientosPorEstado.class, DESDE, HASTA);
        assertArrayEquals(new String[] {"En curso", "2"}, matriz[1]);
        assertArrayEquals(new String[] {"Finalizado", "8"}, matriz[2]);
        assertArrayEquals(new String[] {"Total", "10"}, matriz[3]);
    }

    @Test
    @DisplayName("Período sin datos: solo encabezados y Total 0")
    void periodoSinDatos() throws Exception {
        String[][] matriz = matriz(ReportePorTipo.class, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        assertEquals(2, matriz.length);
        assertArrayEquals(new String[] {"Total", "0"}, matriz[1]);
    }
}
