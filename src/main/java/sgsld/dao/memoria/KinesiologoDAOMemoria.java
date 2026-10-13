package sgsld.dao.memoria;

import java.util.Optional;

import sgsld.dao.KinesiologoDAO;
import sgsld.modelo.Kinesiologo;
import sgsld.util.Texto;

/** Kinesiólogos en memoria. */
public class KinesiologoDAOMemoria extends DAOMemoria<Kinesiologo> implements KinesiologoDAO {

    @Override
    public Optional<Kinesiologo> buscarPorMatricula(String matricula) {
        return buscarPrimero(kinesiologo -> Texto.iguales(kinesiologo.getMatricula(), matricula));
    }

    @Override
    public Optional<Kinesiologo> buscarPorUsuario(int idUsuario) {
        return buscarPrimero(kinesiologo -> kinesiologo.tieneUsuario()
                && kinesiologo.getUsuario().getId() == idUsuario);
    }
}
