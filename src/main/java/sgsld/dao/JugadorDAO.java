package sgsld.dao;

import java.util.List;
import java.util.Optional;

import sgsld.modelo.Jugador;
import sgsld.util.PersistenciaException;

/** Acceso a datos de jugadores. */
public interface JugadorDAO extends DAO<Jugador> {

    /**
     * Busca un jugador por DNI (clave única).
     *
     * @param dni DNI exacto
     * @return el jugador, o vacío si no existe
     */
    Optional<Jugador> buscarPorDni(String dni) throws PersistenciaException;

    /**
     * Busca jugadores cuyo apellido contiene el texto indicado, sin distinguir
     * mayúsculas ni tildes.
     *
     * @param apellido texto a buscar
     * @return los jugadores que coinciden (activos e inactivos)
     */
    List<Jugador> buscarPorApellido(String apellido) throws PersistenciaException;

    /**
     * Lista los jugadores que no fueron dados de baja.
     *
     * @return jugadores activos
     */
    List<Jugador> listarActivos() throws PersistenciaException;
}
