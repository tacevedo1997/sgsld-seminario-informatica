package sgsld.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.Rol;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.Usuario;
import sgsld.modelo.dto.EstadoJugadorDTO;
import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.servicio.Servicios;
import sgsld.servicio.reporte.Reporte;
import sgsld.servicio.reporte.ReporteDuracionPromedio;
import sgsld.servicio.reporte.ReportePorCategoria;
import sgsld.servicio.reporte.ReportePorMes;
import sgsld.servicio.reporte.ReportePorTipo;
import sgsld.servicio.reporte.ReportePorZona;
import sgsld.servicio.reporte.ReporteTratamientosPorEstado;
import sgsld.util.SesionUsuario;

/**
 * Contrato de las consultas que dependen de todo el lote: estado actual (C3) y
 * reportes (C4 a C9) contra el oráculo de la sección 14.6 de la entrega 2,
 * con los DAO en memoria y con MySQL. Ninguna prueba modifica datos.
 */
public abstract class ContratoReportesTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 9, 26);

    /**
     * Fábrica con el lote de prueba sin modificar.
     *
     * @return la fábrica
     */
    protected abstract FabricaDAO fabrica();

    @BeforeEach
    void iniciarSesionComoCoordinador() {
        SesionUsuario.getInstancia().iniciar(new Usuario(1, "mlopez", "-", Rol.COORDINADOR, true));
    }

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    private Servicios servicios() {
        return Servicios.crear(fabrica(), EscenarioPrueba.RELOJ);
    }

    private static Reporte reporte(Servicios servicios, Class<? extends Reporte> clase) throws Exception {
        for (Reporte reporte : servicios.reportes().getReportesDisponibles()) {
            if (reporte.getClass() == clase) {
                return reporte;
            }
        }
        throw new AssertionError("No está disponible " + clase.getSimpleName());
    }

    /** Genera el reporte de la clase indicada y lo resume como "etiqueta cantidad [promedio]". */
    private List<String> generar(Class<? extends Reporte> clase) throws Exception {
        Servicios servicios = servicios();
        List<String> resumen = new ArrayList<>();
        for (FilaReporteDTO fila : servicios.reportes().generar(reporte(servicios, clase), DESDE, HASTA)) {
            resumen.add(fila.etiqueta() + " " + fila.cantidad() + (fila.promedio() == null ? "" : " " + fila.promedio()));
        }
        return resumen;
    }

    @Test
    @DisplayName("C3 · Estado actual: 8 jugadores activos por categoría y jugador; Gómez y Herrera en tratamiento")
    void estadoActual() throws Exception {
        SesionUsuario.getInstancia().iniciar(new Usuario(4, "ctecnico", "-", Rol.CUERPO_TECNICO, true));
        List<String> filas = new ArrayList<>();
        for (EstadoJugadorDTO fila : servicios().consultas().consultarEstadoActual()) {
            filas.add(fila.jugador() + " | " + fila.categoria() + " | " + fila.estado());
        }
        assertEquals(List.of(
                "Gómez, Pedro | Primera | En tratamiento",
                "Molina, Lucas | Primera | Disponible",
                "Pérez, Juan | Primera | Disponible",
                "Acosta, Franco | Reserva | Disponible",
                "Díaz, Luis | Reserva | Disponible",
                "Castro, Nicolás | Sub-15 | Disponible",
                "Herrera, Tomás | Sub-17 | En tratamiento",
                "Romero, Matías | Sub-17 | Disponible"), filas);
    }

    @Test
    @DisplayName("C4 · Por categoría: Sub-17 3 · Primera 2 · Reserva 2 · Sub-15 1")
    void porCategoria() throws Exception {
        assertEquals(List.of("Sub-17 3", "Primera 2", "Reserva 2", "Sub-15 1"), generar(ReportePorCategoria.class));
    }

    @Test
    @DisplayName("C5 · Por zona: Isquiotibiales 3 · Aductores 1 · Cuádriceps 1 · Hombro 1 · Rodilla 1 · Tobillo 1")
    void porZona() throws Exception {
        assertEquals(List.of("Isquiotibiales 3", "Aductores 1", "Cuádriceps 1", "Hombro 1", "Rodilla 1", "Tobillo 1"),
                generar(ReportePorZona.class));
    }

    @Test
    @DisplayName("C6 · Por tipo: Muscular 5 · Contusión 2 · Ligamentaria 1")
    void porTipo() throws Exception {
        assertEquals(List.of("Muscular 5", "Contusión 2", "Ligamentaria 1"), generar(ReportePorTipo.class));
    }

    @Test
    @DisplayName("C7 · Por mes: 2026-01 1 · 2026-03 1 · ... · 2026-08 2")
    void porMes() throws Exception {
        assertEquals(List.of("2026-01 1", "2026-03 1", "2026-04 1", "2026-05 1", "2026-06 1", "2026-07 1", "2026-08 2"),
                generar(ReportePorMes.class));
    }

    @Test
    @DisplayName("C8 · Duración promedio: Muscular 4 tratamientos, 31,5 días · Contusión 2, 12,0 días")
    void duracionPromedio() throws Exception {
        assertEquals(List.of("Muscular 4 31.5", "Contusión 2 12.0"), generar(ReporteDuracionPromedio.class));
    }

    @Test
    @DisplayName("C9 · Por estado: En curso 2 · Finalizado 8")
    void porEstado() throws Exception {
        assertEquals(List.of("En curso 2", "Finalizado 8"), generar(ReporteTratamientosPorEstado.class));
    }

    @Test
    @DisplayName("CU-07 5a · Período sin datos (2024): resultado vacío")
    void periodoSinDatos() throws Exception {
        Servicios servicios = servicios();
        assertTrue(servicios.reportes().generar(reporte(servicios, ReportePorZona.class), LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)).isEmpty());
    }

    @Test
    @DisplayName("Tratamientos en curso: 7 (Gómez) y 9 (Herrera), con su lesión y jugador")
    void tratamientosEnCurso() throws Exception {
        SesionUsuario.getInstancia().iniciar(new Usuario(2, "lfernandez", "-", Rol.KINESIOLOGO, true));
        List<String> enCurso = new ArrayList<>();
        for (Tratamiento tratamiento : servicios().tratamientos().listarEnCurso()) {
            enCurso.add(tratamiento.getId() + " " + tratamiento.getLesion().getId() + " "
                    + tratamiento.getLesion().getJugador().getApellido());
        }
        assertEquals(List.of("7 7 Gómez", "9 9 Herrera"), enCurso);
    }
}
