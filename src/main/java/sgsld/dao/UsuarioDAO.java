package sgsld.dao;

import java.util.Optional;

import sgsld.modelo.Usuario;
import sgsld.util.PersistenciaException;

/** Acceso a datos de usuarios. */
public interface UsuarioDAO extends DAO<Usuario> {

    /**
     * Busca un usuario por su nombre de inicio de sesión, sin distinguir
     * mayúsculas ni tildes.
     *
     * @param nombreUsuario nombre de usuario
     * @return el usuario, o vacío si no existe
     */
    Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) throws PersistenciaException;
}
