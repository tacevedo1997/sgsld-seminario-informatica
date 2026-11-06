package sgsld.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.EstadoTratamiento;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Lesion;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.Tratamiento;
import sgsld.util.ValidacionException;

/** CPC04 y CPC05 · Registro, evolución y finalización de tratamientos (CU-05 y CU-06), y "Registrar y finalizar". */
@DisplayName("CPC04 / CPC05 · Tratamientos")
class TratamientoServiceTest {

    private final EscenarioPrueba escenario = new EscenarioPrueba().comoKinesiologo();

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    private Lesion lesionDelLote(int id) {
        return escenario.fabrica().getLesionDAO().buscarPorId(id).orElseThrow();
    }

    /** Registra una lesión nueva de Lucas Molina (sin lesiones en el lote). */
    private Lesion lesionNueva(LocalDate fecha) throws Exception {
        Lesion lesion = new Lesion(escenario.jugador("39874521"), escenario.tipo(1), escenario.zona(2),
                Gravedad.LEVE, fecha, null);
        escenario.lesiones().registrarLesion(lesion);
        return lesion;
    }

    private Tratamiento tratamiento(Lesion lesion, LocalDate inicio) {
        return new Tratamiento(lesion, escenario.kinesiologo(1), inicio, "Readaptación funcional", null);
    }

    @Test
    @DisplayName("CPC04-1 · Lesión sin tratamientos: queda En curso")
    void lesionSinTratamientos() throws Exception {
        Lesion lesion = lesionNueva(LocalDate.of(2026, 9, 20));
        Tratamiento nuevo = tratamiento(lesion, LocalDate.of(2026, 9, 21));
        int id = escenario.tratamientos().registrarTratamiento(nuevo);
        assertEquals(11, id);
        assertEquals(EstadoTratamiento.EN_CURSO, escenario.tratamientos().buscarPorId(id).getEstado());
        assertEquals(1, lesion.getTratamientos().size());
        assertEquals(EstadoDisponibilidad.EN_TRATAMIENTO,
                escenario.consultas().consultarEstado(lesion.getJugador().getId()));
    }

    @Test
    @DisplayName("CPC04-2 · Lesión con el tratamiento anterior finalizado: queda En curso")
    void lesionConAnteriorFinalizado() throws Exception {
        Lesion lesion2 = lesionDelLote(2);
        Tratamiento nuevo = tratamiento(lesion2, LocalDate.of(2026, 9, 26));
        escenario.tratamientos().registrarTratamiento(nuevo);
        assertTrue(nuevo.estaEnCurso());
        assertEquals(2, lesion2.getTratamientos().size());
        assertSame(nuevo, lesion2.getUltimoTratamiento());
    }

    @Test
    @DisplayName("CPC04-3 · Lesión 7 (ya tiene uno en curso): rechazo")
    void lesionConTratamientoEnCurso() {
        Lesion lesion7 = lesionDelLote(7);
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.tratamientos().registrarTratamiento(tratamiento(lesion7, LocalDate.of(2026, 9, 20))));
        assertTrue(error.getErrores().get(0).contains("ya tiene un tratamiento en curso"));
        assertEquals(1, lesion7.getTratamientos().size());
    }

    @Test
    @DisplayName("CPC04-4 · Inicio anterior a la lesión o futuro: rechazo")
    void inicioFueraDeRango() throws Exception {
        Lesion lesion = lesionNueva(LocalDate.of(2026, 9, 20));
        assertThrows(ValidacionException.class,
                () -> escenario.tratamientos().registrarTratamiento(tratamiento(lesion, LocalDate.of(2026, 9, 19))));
        assertThrows(ValidacionException.class,
                () -> escenario.tratamientos().registrarTratamiento(tratamiento(lesion, LocalDate.of(2026, 9, 27))));
        assertTrue(lesion.getTratamientos().isEmpty());
    }

    @Test
    @DisplayName("CPC05-1 · Fin un día antes del inicio: rechazo")
    void finAntesDelInicio() {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.tratamientos().finalizarTratamiento(9, LocalDate.of(2026, 8, 2)));
        assertTrue(error.getErrores().get(0).startsWith("La fecha de fin debe estar entre"));
        assertTrue(lesionDelLote(9).getUltimoTratamiento().estaEnCurso());
    }

    @Test
    @DisplayName("CPC05-2 · Fin igual al inicio: finalizado con 0 días")
    void finIgualAlInicio() throws Exception {
        Lesion lesion = lesionNueva(LocalDate.of(2026, 9, 26));
        int id = escenario.tratamientos().registrarTratamiento(tratamiento(lesion, LocalDate.of(2026, 9, 26)));
        escenario.tratamientos().finalizarTratamiento(id, LocalDate.of(2026, 9, 26));
        Tratamiento finalizado = escenario.tratamientos().buscarPorId(id);
        assertEquals(EstadoTratamiento.FINALIZADO, finalizado.getEstado());
        assertEquals(0, finalizado.getDuracionDias());
        assertEquals(EstadoDisponibilidad.DISPONIBLE,
                escenario.consultas().consultarEstado(lesion.getJugador().getId()));
    }

    @Test
    @DisplayName("CPC05-3 · Evolución registrada sin confirmar la finalización: sigue En curso (6a)")
    void sinConfirmarFinalizacion() throws Exception {
        escenario.tratamientos().registrarEvolucion(7,
                new Evolucion(LocalDate.of(2026, 9, 25), "Completa trabajo con pelota", ResultadoEvolucion.FAVORABLE));
        Tratamiento tratamiento7 = escenario.tratamientos().buscarPorId(7);
        assertTrue(tratamiento7.estaEnCurso());
        assertEquals(5, tratamiento7.getCantidadEvoluciones());
        assertEquals(EstadoDisponibilidad.EN_TRATAMIENTO, escenario.consultas().consultarEstado(2));
    }

    @Test
    @DisplayName("CU-06 3a · Evolución con fecha fuera de rango (antes del inicio o futura): rechazo")
    void evolucionFueraDeRango() throws Exception {
        assertThrows(ValidacionException.class, () -> escenario.tratamientos().registrarEvolucion(7,
                new Evolucion(LocalDate.of(2026, 6, 15), "Antes del inicio", ResultadoEvolucion.ESTABLE)));
        assertThrows(ValidacionException.class, () -> escenario.tratamientos().registrarEvolucion(7,
                new Evolucion(LocalDate.of(2026, 9, 27), "Fecha futura", ResultadoEvolucion.ESTABLE)));
        assertEquals(4, escenario.tratamientos().buscarPorId(7).getCantidadEvoluciones());
    }

    @Test
    @DisplayName("CU-06 · Evolución en un tratamiento finalizado: rechazo")
    void evolucionEnFinalizado() {
        ValidacionException error = assertThrows(ValidacionException.class, () -> escenario.tratamientos()
                .registrarEvolucion(2, new Evolucion(LocalDate.of(2026, 9, 1), "Control", ResultadoEvolucion.ESTABLE)));
        assertTrue(error.getErrores().contains("Solo se registran evoluciones en tratamientos en curso."));
    }

    @Test
    @DisplayName("CPC05-4 · Registrar y finalizar: evolución y fin en una operación; el jugador queda Disponible")
    void registrarYFinalizar() throws Exception {
        Evolucion ultima = new Evolucion(LocalDate.of(2026, 9, 20), "Estabilidad articular completa; alta",
                ResultadoEvolucion.FAVORABLE);
        int id = escenario.tratamientos().registrarEvolucionYFinalizar(9, ultima, LocalDate.of(2026, 9, 20));
        assertEquals(12, id);
        Tratamiento tratamiento9 = escenario.tratamientos().buscarPorId(9);
        assertEquals(EstadoTratamiento.FINALIZADO, tratamiento9.getEstado());
        assertEquals(48, tratamiento9.getDuracionDias());
        assertSame(ultima, tratamiento9.getEvoluciones().get(2));
        assertEquals(EstadoDisponibilidad.DISPONIBLE, escenario.consultas().consultarEstado(5));
    }

    @Test
    @DisplayName("CPC05-5 · Registrar y finalizar con fin anterior al inicio y a la evolución: rechazo, nada cambia")
    void registrarYFinalizarFinFueraDeRango() {
        Evolucion evolucion = new Evolucion(LocalDate.of(2026, 9, 20), "Control", ResultadoEvolucion.ESTABLE);
        ValidacionException error = assertThrows(ValidacionException.class, () -> escenario.tratamientos()
                .registrarEvolucionYFinalizar(9, evolucion, LocalDate.of(2026, 8, 2)));
        assertEquals(List.of("La fecha de fin debe estar entre el inicio del tratamiento (03/08/2026) y la fecha "
                        + "actual (26/09/2026).",
                "La fecha de fin no puede ser anterior a la fecha de la evolución (20/09/2026)."), error.getErrores());
        Tratamiento tratamiento9 = lesionDelLote(9).getUltimoTratamiento();
        assertTrue(tratamiento9.estaEnCurso());
        assertEquals(2, tratamiento9.getCantidadEvoluciones());
    }

    @Test
    @DisplayName("CPC05-6 · Registrar y finalizar en un tratamiento finalizado o sin fecha de fin: rechazo")
    void registrarYFinalizarSinCondiciones() {
        Evolucion evolucion = new Evolucion(LocalDate.of(2026, 9, 1), "Control", ResultadoEvolucion.ESTABLE);
        ValidacionException finalizado = assertThrows(ValidacionException.class, () -> escenario.tratamientos()
                .registrarEvolucionYFinalizar(2, evolucion, LocalDate.of(2026, 9, 1)));
        assertTrue(finalizado.getErrores().contains("Solo se registran evoluciones en tratamientos en curso."));
        ValidacionException sinFin = assertThrows(ValidacionException.class, () -> escenario.tratamientos()
                .registrarEvolucionYFinalizar(9, evolucion, null));
        assertEquals(1, sinFin.getErrores().size());
        assertEquals(2, lesionDelLote(9).getUltimoTratamiento().getCantidadEvoluciones());
    }
}