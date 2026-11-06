package sgsld.dao;

import java.util.List;

import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.dto.EstadoJugadorDTO;
import sgsld.util.PersistenciaException;

/**
 * Estado de disponibilidad de los jugadores (equivale a la vista
 * {@code v_estado_jugador}). Es el único lugar donde se deriva el estado: "En
 * tratamiento" si el jugador tiene al menos un tratamiento en curso;
 * "Disponible" en caso contrario.
 */
public interface EstadoJugadorDAO {

    /**
     * Lista el estado de los jugadores activos.
     *
     * @return una fila por jugador activo (sin orden garantizado)
     */
    List<EstadoJugadorDTO> listarEstados() throws PersistenciaException;

    /**
     * Deriva el estado de un jugador.
     *
     * @param idJugador identificador del jugador
     * @return su estado de disponibilidad
     */
    EstadoDisponibilidad estadoDe(int idJugador) throws PersistenciaException;
}
