package sgsld.servicio.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.util.ValidacionException;

/** CPI03 · Los seis reportes contra el oráculo de la sección 14.6 (consultas C4 a C9). */
@DisplayName("CPI03 · Reportes contra el oráculo (01/01/2026 a 26/09/2026)")
class ReportesOraculoTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 9, 26);

    private final EscenarioPrueba escenario = new EscenarioPrueba().comoCoordinador();

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    private Reporte reporte(Class<? extends Reporte> clase) throws Exception {
        for (Reporte reporte : escenario.reportes().getReportesDisponibles()) {
            if (reporte.getClass() == clase) {
                return reporte;
            }
        }
        throw new AssertionError("No está disponible " + clase.getSimpleName());
    }

    /** Genera el reporte y lo resume como "etiqueta cantidad [promedio]". */
    private List<String> generar(Class<? extends Reporte> clase) throws Exception {
        List<String> resumen = new ArrayList<>();
        for (FilaReporteDTO fila : escenario.reportes().generar(reporte(clase), DESDE, HASTA)) {
            resumen.add(fila.etiqueta() + " " + fila.cantidad() + (fila.promedio() == null ? "" : " " + fila.promedio()));
        }
        return resumen;
    }

    @Test
    @DisplayName("CPI03-0 · Seis reportes disponibles, en el orden del menú")
    void reportesDisponibles() throws Exception {
        List<String> titulos = new ArrayList<>();
        for (Reporte reporte : escenario.reportes().getReportesDisponibles()) {
            titulos.add(reporte.getTitulo());
        }
        assertEquals(List.of("Lesiones por categoría", "Lesiones por zona corporal", "Lesiones por tipo",
                "Lesiones por mes", "Duración promedio de tratamientos finalizados por tipo de lesión",
                "Tratamientos en curso y finalizados"), titulos);
    }

    @Test
    @DisplayName("CPI03-1 · Por categoría (C4): Sub-17 3 · Primera 2 · Reserva 2 · Sub-15 1")
    void porCategoria() throws Exception {
        assertEquals(List.of("Sub-17 3", "Primera 2", "Reserva 2", "Sub-15 1"), generar(ReportePorCategoria.class));
    }

    @Test
    @DisplayName("CPI03-2 · Por zona (C5): Isquiotibiales 3 · Aductores 1 · Cuádriceps 1 · Hombro 1 · Rodilla 1 · Tobillo 1")
    void porZona() throws Exception {
        assertEquals(List.of("Isquiotibiales 3", "Aductores 1", "Cuádriceps 1", "Hombro 1", "Rodilla 1", "Tobillo 1"),
                generar(ReportePorZona.class));
    }

    @Test
    @DisplayName("CPI03-3 · Por tipo (C6): Muscular 5 · Contusión 2 · Ligamentaria 1")
    void porTipo() throws Exception {
        assertEquals(List.of("Muscular 5", "Contusión 2", "Ligamentaria 1"), generar(ReportePorTipo.class));
    }

    @Test
    @DisplayName("CPI03-4 · Por mes (C7): 2026-01 1 · 2026-03 1 · ... · 2026-08 2")
    void porMes() throws Exception {
        assertEquals(List.of("2026-01 1", "2026-03 1", "2026-04 1", "2026-05 1", "2026-06 1", "2026-07 1", "2026-08 2"),
                generar(ReportePorMes.class));
    }

    @Test
    @DisplayName("CPI03-5 · Duración promedio (C8): Muscular 4 tratamientos, 31,5 días · Contusión 2, 12,0 días")
    void duracionPromedio() throws Exception {
        assertEquals(List.of("Muscular 4 31.5", "Contusión 2 12.0"), generar(ReporteDuracionPromedio.class));
        Reporte reporte = reporte(ReporteDuracionPromedio.class);
        FilaReporteDTO muscular = escenario.reportes().generar(reporte, DESDE, HASTA).get(0);
        assertEquals(List.of("Muscular", "4", "31,5"), reporte.getCeldas(muscular));
    }

    @Test
    @DisplayName("CPI03-6 · Por estado (C9): En curso 2 · Finalizado 8 (foto actual, sin período)")
    void porEstado() throws Exception {
        assertEquals(List.of("En curso 2", "Finalizado 8"), generar(ReporteTratamientosPorEstado.class));
        assertEquals(false, reporte(ReporteTratamientosPorEstado.class).usaPeriodo());
    }

    @Test
    @DisplayName("CPI03-7 · Total de lesiones del período en los conteos: 8")
    void totalDeLesiones() throws Exception {
        for (Class<? extends Reporte> clase : List.of(ReportePorCategoria.class, ReportePorZona.class,
                ReportePorTipo.class, ReportePorMes.class)) {
            long total = 0;
            for (FilaReporteDTO fila : escenario.reportes().generar(reporte(clase), DESDE, HASTA)) {
                total += fila.cantidad();
            }
            assertEquals(8, total, clase.getSimpleName());
        }
    }

    @Test
    @DisplayName("CU-07 5a · Período sin datos: resultado vacío")
    void periodoSinDatos() throws Exception {
        assertTrue(escenario.reportes().generar(reporte(ReportePorZona.class),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)).isEmpty());
    }

    @Test
    @DisplayName("CU-07 · Período invertido (desde posterior a hasta): rechazo")
    void periodoInvertido() {
        assertThrows(ValidacionException.class,
                () -> escenario.reportes().generar(reporte(ReportePorZona.class), HASTA, DESDE));
    }
}
