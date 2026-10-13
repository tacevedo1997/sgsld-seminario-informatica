package sgsld.servicio;

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
import sgsld.modelo.Categoria;
import sgsld.modelo.Jugador;
import sgsld.modelo.dto.EstadoJugadorDTO;
import sgsld.util.AccesoDenegadoException;

/** CPS01 · Control de acceso por rol (RNF07, RF12) y B1 · baja lógica. */
@DisplayName("CPS01 · Control de acceso y B1 · baja lógica")
class ControlAccesoTest {

    /** Resultado de la consulta C3 con el lote: "jugador | categoría | estado". */
    private static final List<String> ORACULO_C3 = List.of(
            "Gómez, Pedro | Primera | En tratamiento",
            "Molina, Lucas | Primera | Disponible",
            "Pérez, Juan | Primera | Disponible",
            "Acosta, Franco | Reserva | Disponible",
            "Díaz, Luis | Reserva | Disponible",
            "Castro, Nicolás | Sub-15 | Disponible",
            "Herrera, Tomás | Sub-17 | En tratamiento",
            "Romero, Matías | Sub-17 | Disponible");

    private final EscenarioPrueba escenario = new EscenarioPrueba();

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    private static List<String> comoTexto(List<EstadoJugadorDTO> filas) {
        List<String> texto = new ArrayList<>();
        for (EstadoJugadorDTO fila : filas) {
            texto.add(fila.jugador() + " | " + fila.categoria() + " | " + fila.estado());
        }
        return texto;
    }

    @Test
    @DisplayName("CPS01-1 · Cuerpo Técnico: consultarFicha lanza AccesoDenegadoException")
    void cuerpoTecnicoSinFicha() {
        escenario.comoCuerpoTecnico();
        assertThrows(AccesoDenegadoException.class, () -> escenario.consultas().consultarFicha(2));
    }

    @Test
    @DisplayName("CPS01-2 · Cuerpo Técnico: estado actual con las 8 filas de la consulta C3, en orden")
    void cuerpoTecnicoVeEstado() throws Exception {
        escenario.comoCuerpoTecnico();
        assertEquals(ORACULO_C3, comoTexto(escenario.consultas().consultarEstadoActual()));
    }

    @Test
    @DisplayName("CPS01-3 · Cuerpo Técnico: tampoco accede a historial, jugadores ni registro de lesiones")
    void cuerpoTecnicoSinDatosClinicos() {
        escenario.comoCuerpoTecnico();
        assertThrows(AccesoDenegadoException.class, () -> escenario.lesiones().consultarHistorial(2));
        assertThrows(AccesoDenegadoException.class, () -> escenario.jugadores().listar(false));
        assertThrows(AccesoDenegadoException.class, () -> escenario.tratamientos().listarEnCurso());
    }

    @Test
    @DisplayName("CPS01-4 · Kinesiólogo no administra; Coordinador no registra lesiones; sin sesión, nada")
    void separacionDeRoles() throws Exception {
        Categoria primera = escenario.fabrica().getCategoriaDAO().buscarPorId(1).orElseThrow();
        Jugador nuevo = new Jugador("40000000", "Martín", "Suárez", LocalDate.of(2001, 5, 10), null,
                "Defensor", primera);
        assertThrows(AccesoDenegadoException.class, () -> escenario.consultas().consultarEstadoActual());
        escenario.comoKinesiologo();
        assertThrows(AccesoDenegadoException.class, () -> escenario.jugadores().registrar(nuevo));
        assertEquals(2, escenario.consultas().consultarFicha(2).historial().size());
        escenario.comoCoordinador();
        assertThrows(AccesoDenegadoException.class, () -> escenario.lesiones().listarZonas());
        assertEquals(2, escenario.consultas().consultarFicha(2).historial().size());
    }

    @Test
    @DisplayName("B1 · Tras la baja lógica de DNI 39874521 hay 7 filas y Molina ya no aparece")
    void bajaLogica() throws Exception {
        escenario.comoCoordinador();
        Jugador molina = escenario.jugadores().buscarPorDni("39874521");
        escenario.jugadores().darDeBaja(molina.getId());
        escenario.comoCuerpoTecnico();
        List<String> filas = comoTexto(escenario.consultas().consultarEstadoActual());
        assertEquals(7, filas.size());
        assertTrue(filas.stream().noneMatch(fila -> fila.startsWith("Molina")));
        List<String> esperado = new ArrayList<>(ORACULO_C3);
        esperado.remove("Molina, Lucas | Primera | Disponible");
        assertEquals(esperado, filas);
    }
}
