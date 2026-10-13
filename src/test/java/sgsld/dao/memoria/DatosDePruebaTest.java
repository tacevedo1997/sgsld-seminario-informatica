package sgsld.dao.memoria;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.dao.FabricaDAO;
import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.dto.EstadoJugadorDTO;

/** Verifica que el lote en memoria coincide con los INSERT de sgsld_script.sql. */
@DisplayName("DatosDePrueba · verificación de la carga (como el SELECT COUNT del script)")
class DatosDePruebaTest {

    private FabricaDAO fabrica;

    @BeforeEach
    void cargar() {
        fabrica = new FabricaDAOMemoria();
        DatosDePrueba.cargar(fabrica);
    }

    @Test
    @DisplayName("Cantidad de registros por tabla")
    void cantidades() {
        assertEquals(4, fabrica.getCategoriaDAO().listarTodos().size());
        assertEquals(4, fabrica.getUsuarioDAO().listarTodos().size());
        assertEquals(3, fabrica.getKinesiologoDAO().listarTodos().size());
        assertEquals(7, fabrica.getZonaCorporalDAO().listarTodos().size());
        assertEquals(5, fabrica.getTipoLesionDAO().listarTodos().size());
        assertEquals(8, fabrica.getJugadorDAO().listarTodos().size());
        assertEquals(10, fabrica.getLesionDAO().listarTodos().size());
        assertEquals(10, fabrica.getTratamientoDAO().listarTodos().size());
        int evoluciones = 0;
        for (Tratamiento tratamiento : fabrica.getTratamientoDAO().listarTodos()) {
            evoluciones += fabrica.getEvolucionDAO().buscarPorTratamiento(tratamiento.getId()).size();
        }
        assertEquals(11, evoluciones);
    }

    @Test
    @DisplayName("Tratamientos 7 y 9 en curso; el resto finalizados con su fecha de fin")
    void estadosDeTratamientos() {
        Set<Integer> enCurso = new HashSet<>();
        for (Tratamiento tratamiento : fabrica.getTratamientoDAO().listarEnCurso()) {
            enCurso.add(tratamiento.getId());
        }
        assertEquals(Set.of(7, 9), enCurso);
        assertEquals(40, fabrica.getTratamientoDAO().buscarPorId(2).orElseThrow().getDuracionDias());
        assertEquals(4, fabrica.getTratamientoDAO().buscarPorId(7).orElseThrow().getCantidadEvoluciones());
    }

    @Test
    @DisplayName("Vista de estado: 8 jugadores activos, Gómez y Herrera en tratamiento")
    void vistaDeEstado() {
        Set<String> enTratamiento = new HashSet<>();
        for (EstadoJugadorDTO fila : fabrica.getEstadoJugadorDAO().listarEstados()) {
            if (fila.estado() == EstadoDisponibilidad.EN_TRATAMIENTO) {
                enTratamiento.add(fila.jugador());
            }
        }
        assertEquals(8, fabrica.getEstadoJugadorDAO().listarEstados().size());
        assertEquals(Set.of("Gómez, Pedro", "Herrera, Tomás"), enTratamiento);
    }
}
