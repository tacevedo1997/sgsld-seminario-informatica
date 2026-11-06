package sgsld.dao.memoria;

import java.util.Optional;

import sgsld.dao.KinesiologoDAO;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Usuario;
import sgsld.util.Texto;

/** Kinesiólogos en memoria. */
public class KinesiologoDAOMemoria extends DAOMemoria<Kinesiologo> implements KinesiologoDAO {

    private final UsuarioDAOMemoria usuarios;

    /**
     * Crea el DAO, que guarda los usuarios nuevos en la tabla de usuarios en memoria.
     *
     * @param usuarios DAO de usuarios en memoria
     */
    public KinesiologoDAOMemoria(UsuarioDAOMemoria usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<Kinesiologo> buscarPorMatricula(String matricula) {
        return buscarPrimero(kinesiologo -> Texto.iguales(kinesiologo.getMatricula(), matricula));
    }

    @Override
    public Optional<Kinesiologo> buscarPorUsuario(int idUsuario) {
        return buscarPrimero(kinesiologo -> kinesiologo.tieneUsuario()
                && kinesiologo.getUsuario().getId() == idUsuario);
    }

    /** En memoria no hay transacciones: el servicio ya validó la unicidad de ambos. */
    @Override
    public int insertarConUsuario(Kinesiologo kinesiologo, Usuario usuario) {
        usuarios.insertar(usuario);
        kinesiologo.setUsuario(usuario);
        return insertar(kinesiologo);
    }
}
