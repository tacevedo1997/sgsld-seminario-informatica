package sgsld.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Lesion;
import sgsld.modelo.ZonaCorporal;
import sgsld.util.ValidacionException;

/** CPC03 · Validación de los datos de una lesión (CU-04, alternativos 4a y 4b). */
@DisplayName("CPC03 · Validación de lesiones")
class ValidacionLesionTest {

    private final EscenarioPrueba escenario = new EscenarioPrueba().comoKinesiologo();
    private final Jugador perez = escenario.jugador("41236587");
    private final ZonaCorporal tobillo = escenario.zona(3);

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    private Lesion lesion(LocalDate fecha, ZonaCorporal zona, String descripcion) {
        return new Lesion(perez, escenario.tipo(2), zona, Gravedad.LEVE, fecha, descripcion);
    }

    @Test
    @DisplayName("CPC03-1 · Fecha 26/09/2026 (hoy): se registra")
    void fechaDeHoy() throws Exception {
        int id = escenario.lesiones().registrarLesion(lesion(LocalDate.of(2026, 9, 26), tobillo, "Esguince leve"));
        assertEquals(11, id);
        assertEquals(2, escenario.lesiones().consultarHistorial(perez.getId()).size());
    }

    @Test
    @DisplayName("CPC03-2 · Fecha 27/09/2026 (futura): rechazo")
    void fechaFutura() {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.lesiones().registrarLesion(lesion(LocalDate.of(2026, 9, 27), tobillo, null)));
        assertEquals(1, error.getErrores().size());
        assertTrue(error.getErrores().get(0).startsWith("La fecha de la lesión no puede ser posterior"));
    }

    @Test
    @DisplayName("CPC03-3 · Fecha vacía: rechazo")
    void fechaVacia() {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.lesiones().registrarLesion(lesion(null, tobillo, null)));
        assertEquals(List.of("El campo fecha es obligatorio."), error.getErrores());
    }

    @Test
    @DisplayName("CPC03-4 · Zona sin seleccionar: rechazo")
    void zonaSinSeleccionar() {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.lesiones().registrarLesion(lesion(LocalDate.of(2026, 9, 26), null, null)));
        assertEquals(List.of("El campo zona corporal es obligatorio."), error.getErrores());
    }

    @Test
    @DisplayName("CPC03-5 · Descripción de 501 caracteres: rechazo (500 se acepta)")
    void descripcionDemasiadoLarga() throws Exception {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.lesiones().registrarLesion(lesion(LocalDate.of(2026, 9, 26), tobillo, "x".repeat(501))));
        assertEquals(List.of("El campo descripción admite hasta 500 caracteres."), error.getErrores());
        escenario.lesiones().registrarLesion(lesion(LocalDate.of(2026, 9, 26), tobillo, "x".repeat(500)));
    }

    @Test
    @DisplayName("CPC03-6 · Varios errores a la vez: se informan todos juntos")
    void variosErrores() {
        Lesion vacia = new Lesion(perez, null, null, null, null, null);
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.lesiones().registrarLesion(vacia));
        assertEquals(4, error.getErrores().size());
    }
}
