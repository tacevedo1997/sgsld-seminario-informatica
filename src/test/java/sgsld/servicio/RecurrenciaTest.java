package sgsld.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Lesion;
import sgsld.modelo.TipoLesion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.ZonaCorporal;

/** CPC01 y CPC02 · Regla de recurrencia (RF06, CU-11). */
@DisplayName("Recurrencia · CPC01, CPC02, fin de mes y oráculo C2")
class RecurrenciaTest {

    private final EscenarioPrueba escenario = new EscenarioPrueba().comoKinesiologo();
    private final ZonaCorporal isquiotibiales = escenario.zona(1);
    private final ZonaCorporal tobillo = escenario.zona(3);
    private final TipoLesion muscular = escenario.tipo(1);

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    private Lesion lesion(Jugador jugador, ZonaCorporal zona, LocalDate fecha) {
        return new Lesion(jugador, muscular, zona, Gravedad.LEVE, fecha, null);
    }

    private Tratamiento tratar(Lesion lesion, LocalDate inicio) {
        Tratamiento tratamiento = new Tratamiento(lesion, escenario.kinesiologo(1), inicio, "Fisioterapia", null);
        lesion.agregarTratamiento(tratamiento);
        return tratamiento;
    }

    private Optional<Lesion> verificar(Lesion nueva, List<Lesion> historial) throws Exception {
        return escenario.lesiones().verificarRecurrencia(nueva, historial);
    }

    /** Verifica una lesión nueva contra los antecedentes reales del jugador. */
    private Optional<Lesion> alertaPara(Jugador jugador, ZonaCorporal zona, LocalDate fecha) throws Exception {
        Lesion nueva = lesion(jugador, zona, fecha);
        return verificar(nueva, escenario.lesiones().buscarAntecedentes(nueva));
    }

    @Nested
    @DisplayName("CPC01 · Caminos independientes de verificarRecurrencia (V(G) = 6)")
    class Caminos {

        private final Jugador jugador = new Jugador("40000000", "Martín", "Suárez", LocalDate.of(2001, 5, 10),
                null, "Defensor", null);
        private final Lesion nueva = lesion(jugador, isquiotibiales, LocalDate.of(2026, 7, 15));

        @Test
        @DisplayName("CPC01-1 · Historial vacío: sin alerta")
        void historialVacio() throws Exception {
            assertTrue(verificar(nueva, List.of()).isEmpty());
        }

        @Test
        @DisplayName("CPC01-2 · Antecedente en otra zona: sin alerta")
        void otraZona() throws Exception {
            Lesion anterior = lesion(jugador, tobillo, LocalDate.of(2026, 6, 1));
            assertTrue(verificar(nueva, List.of(anterior)).isEmpty());
        }

        @Test
        @DisplayName("CPC01-3 · Misma zona sin tratamiento: alerta")
        void sinTratamiento() throws Exception {
            Lesion anterior = lesion(jugador, isquiotibiales, LocalDate.of(2026, 6, 1));
            assertSame(anterior, verificar(nueva, List.of(anterior)).orElseThrow());
        }

        @Test
        @DisplayName("CPC01-4 · Misma zona con tratamiento en curso: alerta")
        void tratamientoEnCurso() throws Exception {
            Lesion anterior = lesion(jugador, isquiotibiales, LocalDate.of(2025, 12, 1));
            tratar(anterior, LocalDate.of(2025, 12, 2));
            assertSame(anterior, verificar(nueva, List.of(anterior)).orElseThrow());
        }

        @Test
        @DisplayName("CPC01-5 · Finalizado dentro de la ventana de seis meses: alerta")
        void finalizadoDentro() throws Exception {
            Lesion anterior = lesion(jugador, isquiotibiales, LocalDate.of(2025, 12, 1));
            tratar(anterior, LocalDate.of(2025, 12, 2)).finalizar(LocalDate.of(2026, 1, 20));
            assertSame(anterior, verificar(nueva, List.of(anterior)).orElseThrow());
        }

        @Test
        @DisplayName("CPC01-6 · Finalizado fuera de la ventana de seis meses: sin alerta")
        void finalizadoFuera() throws Exception {
            Lesion anterior = lesion(jugador, isquiotibiales, LocalDate.of(2025, 12, 1));
            tratar(anterior, LocalDate.of(2025, 12, 2)).finalizar(LocalDate.of(2026, 1, 10));
            assertTrue(verificar(nueva, List.of(anterior)).isEmpty());
        }
    }

    @Nested
    @DisplayName("CPC02 · Valores límite (antecedente 01/12/2025, tratamiento finalizado el 15/01/2026)")
    class ValoresLimite {

        private final Jugador molina = escenario.jugador("39874521");

        /** Registra con los servicios el antecedente de Lucas Molina, que no tiene lesiones en el lote. */
        private Lesion registrarAntecedente(boolean finalizar) throws Exception {
            Lesion antecedente = lesion(molina, isquiotibiales, LocalDate.of(2025, 12, 1));
            escenario.lesiones().registrarLesion(antecedente);
            Tratamiento tratamiento = new Tratamiento(antecedente, escenario.kinesiologo(1),
                    LocalDate.of(2025, 12, 2), "Fisioterapia", null);
            int id = escenario.tratamientos().registrarTratamiento(tratamiento);
            if (finalizar) {
                escenario.tratamientos().finalizarTratamiento(id, LocalDate.of(2026, 1, 15));
            }
            return antecedente;
        }

        @Test
        @DisplayName("CPC02-1 · Nueva lesión el 10/01/2026: alerta")
        void diezDeEnero() throws Exception {
            Lesion antecedente = registrarAntecedente(true);
            assertSame(antecedente, alertaPara(molina, isquiotibiales, LocalDate.of(2026, 1, 10)).orElseThrow());
        }

        @Test
        @DisplayName("CPC02-2 · Nueva lesión el 15/01/2026 (día del alta): alerta")
        void diaDelAlta() throws Exception {
            Lesion antecedente = registrarAntecedente(true);
            assertSame(antecedente, alertaPara(molina, isquiotibiales, LocalDate.of(2026, 1, 15)).orElseThrow());
        }

        @Test
        @DisplayName("CPC02-3 · Nueva lesión el 14/07/2026 (un día antes del límite): alerta")
        void unDiaAntesDelLimite() throws Exception {
            Lesion antecedente = registrarAntecedente(true);
            assertSame(antecedente, alertaPara(molina, isquiotibiales, LocalDate.of(2026, 7, 14)).orElseThrow());
        }

        @Test
        @DisplayName("CPC02-4 · Exactamente seis meses (límite incluido): alerta")
        void exactamenteSeisMeses() throws Exception {
            Lesion antecedente = registrarAntecedente(true);
            assertSame(antecedente, alertaPara(molina, isquiotibiales, LocalDate.of(2026, 7, 15)).orElseThrow());
        }

        @Test
        @DisplayName("CPC02-5 · Seis meses y un día (16/07/2026): sin alerta")
        void seisMesesYUnDia() throws Exception {
            registrarAntecedente(true);
            assertTrue(alertaPara(molina, isquiotibiales, LocalDate.of(2026, 7, 16)).isEmpty());
        }

        @Test
        @DisplayName("CPC02-6 · Otra zona (tobillo, 15/07/2026): sin alerta")
        void otraZona() throws Exception {
            registrarAntecedente(true);
            assertTrue(alertaPara(molina, tobillo, LocalDate.of(2026, 7, 15)).isEmpty());
        }

        @Test
        @DisplayName("CPC02-7 · Tratamiento anterior en curso (20/09/2026): alerta")
        void tratamientoAnteriorEnCurso() throws Exception {
            Lesion antecedente = registrarAntecedente(false);
            assertSame(antecedente, alertaPara(molina, isquiotibiales, LocalDate.of(2026, 9, 20)).orElseThrow());
        }

        @Test
        @DisplayName("CPC02-8 · Jugador sin antecedentes: sin alerta")
        void sinAntecedentes() throws Exception {
            assertTrue(alertaPara(molina, isquiotibiales, LocalDate.of(2026, 7, 15)).isEmpty());
        }

        @Test
        @DisplayName("CPC02-9 · Otro jugador (15/07/2026): sin alerta")
        void otroJugador() throws Exception {
            registrarAntecedente(true);
            assertTrue(alertaPara(escenario.jugador("45120398"), isquiotibiales, LocalDate.of(2026, 7, 15)).isEmpty());
        }
    }

    @Nested
    @DisplayName("Fin de mes (sección 11.4): minusMonths equivale a DATE_SUB")
    class FinDeMes {

        @Test
        @DisplayName("31/08/2026 menos seis meses es 28/02/2026")
        void limiteFinDeMes() {
            assertEquals(LocalDate.of(2026, 2, 28),
                    LocalDate.of(2026, 8, 31).minusMonths(LesionService.MESES_VENTANA));
        }

        @Test
        @DisplayName("Lesión del 31/08/2026: alerta si el alta fue el 28/02/2026, no si fue el 27/02/2026")
        void alertaEnElLimite() throws Exception {
            Jugador jugador = escenario.jugador("39874521");
            Lesion nueva = lesion(jugador, isquiotibiales, LocalDate.of(2026, 8, 31));
            Lesion altaEnLimite = lesion(jugador, isquiotibiales, LocalDate.of(2026, 1, 10));
            tratar(altaEnLimite, LocalDate.of(2026, 1, 11)).finalizar(LocalDate.of(2026, 2, 28));
            Lesion altaAntes = lesion(jugador, isquiotibiales, LocalDate.of(2026, 1, 10));
            tratar(altaAntes, LocalDate.of(2026, 1, 11)).finalizar(LocalDate.of(2026, 2, 27));
            assertSame(altaEnLimite, verificar(nueva, List.of(altaEnLimite)).orElseThrow());
            assertTrue(verificar(nueva, List.of(altaAntes)).isEmpty());
        }
    }

    @Nested
    @DisplayName("Oráculo C2 con el lote de prueba")
    class OraculoC2 {

        @Test
        @DisplayName("C2 caso A · Pedro Gómez, isquiotibiales, 15/06/2026: antecedente lesión 2 (fin 20/02/2026, límite 15/12/2025)")
        void casoA() throws Exception {
            LocalDate fecha = LocalDate.of(2026, 6, 15);
            Lesion antecedente = alertaPara(escenario.jugador("42587634"), isquiotibiales, fecha).orElseThrow();
            assertEquals(2, antecedente.getId());
            assertEquals(LocalDate.of(2026, 2, 20), antecedente.getUltimoTratamiento().getFechaFin());
            assertEquals(LocalDate.of(2025, 12, 15), fecha.minusMonths(LesionService.MESES_VENTANA));
        }

        @Test
        @DisplayName("C2 caso B · Franco Acosta, isquiotibiales, 01/07/2026: sin alerta")
        void casoB() throws Exception {
            assertTrue(alertaPara(escenario.jugador("46078912"), isquiotibiales, LocalDate.of(2026, 7, 1)).isEmpty());
        }

        @Test
        @DisplayName("C2 · Pedro Gómez, isquiotibiales, 26/09/2026: antecedente lesión 7 (tratamiento en curso)")
        void pedroGomezHoy() throws Exception {
            Lesion antecedente = alertaPara(escenario.jugador("42587634"), isquiotibiales,
                    LocalDate.of(2026, 9, 26)).orElseThrow();
            assertEquals(7, antecedente.getId());
            assertTrue(antecedente.getUltimoTratamiento().estaEnCurso());
        }
    }
}
