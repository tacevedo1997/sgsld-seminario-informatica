package sgsld.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;
import sgsld.util.Algoritmos;

/** CPI02 · Historial clínico de un jugador en orden cronológico (CU-12, consulta C1). */
@DisplayName("CPI02 · Historial")
class HistorialTest {

    private final EscenarioPrueba escenario = new EscenarioPrueba().comoKinesiologo();

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    private static List<LocalDate> fechasDeEvoluciones(List<Lesion> historial) {
        List<LocalDate> fechas = new ArrayList<>();
        for (Lesion lesion : historial) {
            for (Tratamiento tratamiento : lesion.getTratamientos()) {
                for (Evolucion evolucion : tratamiento.getEvoluciones()) {
                    fechas.add(evolucion.getFecha());
                }
            }
        }
        return fechas;
    }

    @Test
    @DisplayName("CPI02-1 · Pedro Gómez: 2 lesiones y 7 evoluciones en orden cronológico")
    void historialDePedroGomez() throws Exception {
        List<Lesion> historial = escenario.lesiones().consultarHistorial(escenario.jugador("42587634").getId());
        assertEquals(2, historial.size());
        assertEquals(LocalDate.of(2026, 1, 10), historial.get(0).getFecha());
        assertEquals(LocalDate.of(2026, 6, 15), historial.get(1).getFecha());
        List<LocalDate> fechas = fechasDeEvoluciones(historial);
        assertEquals(7, fechas.size());
        List<LocalDate> ordenadas = new ArrayList<>(fechas);
        Algoritmos.ordenarPorInsercion(ordenadas, LocalDate::compareTo);
        assertEquals(ordenadas, fechas);
        assertEquals(LocalDate.of(2026, 1, 25), fechas.get(0));
        assertEquals(LocalDate.of(2026, 9, 18), fechas.get(6));
    }

    @Test
    @DisplayName("CPI02-2 · Juan Pérez: 1 lesión")
    void historialDeJuanPerez() throws Exception {
        assertEquals(1, escenario.lesiones().consultarHistorial(escenario.jugador("41236587").getId()).size());
    }

    @Test
    @DisplayName("CPI02-3 · Una lesión registrada después con fecha anterior queda primera")
    void ordenaPorFecha() throws Exception {
        Lesion previa = new Lesion(escenario.jugador("42587634"), escenario.tipo(5), escenario.zona(4),
                Gravedad.LEVE, LocalDate.of(2025, 12, 20), "Golpe en la rodilla");
        escenario.lesiones().registrarLesion(previa);
        List<Lesion> historial = escenario.lesiones().consultarHistorial(previa.getJugador().getId());
        assertEquals(3, historial.size());
        assertEquals(previa, historial.get(0));
    }

    @Test
    @DisplayName("CPI02-4 · Jugador sin lesiones (Lucas Molina): historial vacío")
    void sinLesiones() throws Exception {
        List<Lesion> historial = escenario.lesiones().consultarHistorial(escenario.jugador("39874521").getId());
        assertTrue(historial.isEmpty());
    }
}
