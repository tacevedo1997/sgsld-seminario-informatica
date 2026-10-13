package sgsld.dao.memoria;

import java.util.Optional;

import sgsld.dao.UsuarioDAO;
import sgsld.modelo.Usuario;
import sgsld.util.Texto;

/** Usuarios en memoria. */
public class UsuarioDAOMemoria extends DAOMemoria<Usuario> implements UsuarioDAO {

    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) {
        return buscarPrimero(usuario -> Texto.iguales(usuario.getNombreUsuario(), nombreUsuario));
    }
}
