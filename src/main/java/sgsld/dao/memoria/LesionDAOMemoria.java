package sgsld.dao.memoria;

import java.util.List;

import sgsld.dao.LesionDAO;
import sgsld.modelo.Lesion;

/** Lesiones en memoria. */
public class LesionDAOMemoria extends DAOMemoria<Lesion> implements LesionDAO {

    @Override
    public List<Lesion> buscarPorJugador(int idJugador) {
        return filtrar(lesion -> lesion.getJugador() != null && lesion.getJugador().getId() == idJugador);
    }
}
