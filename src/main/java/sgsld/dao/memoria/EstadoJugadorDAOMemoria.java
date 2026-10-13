package sgsld.dao.memoria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sgsld.dao.EstadoJugadorDAO;
import sgsld.dao.JugadorDAO;
import sgsld.dao.TratamientoDAO;
import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.dto.EstadoJugadorDTO;

/**
 * Equivalente en memoria de la vista {@code v_estado_jugador}: deriva el estado
 * de disponibilidad a partir de los tratamientos en curso, sin almacenarlo.
 */
public class EstadoJugadorDAOMemoria implements EstadoJugadorDAO {

    private final JugadorDAO jugadores;
    private final TratamientoDAO tratamientos;

    /**
     * Crea la vista sobre las tablas de jugadores y tratamientos.
     *
     * @param jugadores    DAO de jugadores
     * @param tratamientos DAO de tratamientos
     */
    public EstadoJugadorDAOMemoria(JugadorDAO jugadores, TratamientoDAO tratamientos) {
        this.jugadores = jugadores;
        this.tratamientos = tratamientos;
    }

    @Override
    public List<EstadoJugadorDTO> listarEstados() {
        Set<Integer> enTratamiento = jugadoresEnTratamiento();
        List<EstadoJugadorDTO> filas = new ArrayList<>();
        for (Jugador jugador : jugadores.listarActivos()) {
            filas.add(new EstadoJugadorDTO(jugador.getId(), jugador.getNombreCompleto(),
                    jugador.getCategoria().getNombre(), derivar(enTratamiento, jugador.getId())));
        }
        return filas;
    }

    @Override
    public EstadoDisponibilidad estadoDe(int idJugador) {
        return derivar(jugadoresEnTratamiento(), idJugador);
    }

    /** "En tratamiento" si tiene al menos un tratamiento en curso; si no, "Disponible". */
    private static EstadoDisponibilidad derivar(Set<Integer> enTratamiento, int idJugador) {
        return enTratamiento.contains(idJugador)
                ? EstadoDisponibilidad.EN_TRATAMIENTO
                : EstadoDisponibilidad.DISPONIBLE;
    }

    private Set<Integer> jugadoresEnTratamiento() {
        Set<Integer> ids = new HashSet<>();
        for (Tratamiento tratamiento : tratamientos.listarEnCurso()) {
            ids.add(tratamiento.getLesion().getJugador().getId());
        }
        return ids;
    }
}
