package sgsld.dao;

import java.util.List;

import sgsld.modelo.Lesion;

/** Acceso a datos de lesiones. */
public interface LesionDAO extends DAO<Lesion> {

    /**
     * Lista las lesiones de un jugador, con sus tratamientos y evoluciones.
     *
     * @param idJugador identificador del jugador
     * @return las lesiones del jugador (sin orden garantizado)
     */
    List<Lesion> buscarPorJugador(int idJugador);
}
