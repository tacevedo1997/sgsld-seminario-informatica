package sgsld.dao.mysql;

import java.util.List;
import java.util.Optional;

import sgsld.dao.UsuarioDAO;
import sgsld.modelo.Usuario;
import sgsld.util.PersistenciaException;

/**
 * Usuarios en MySQL (tabla {@code usuario}). Lee el hash PBKDF2 tal como lo
 * guarda el script: las contraseñas nunca se guardan ni se leen en claro.
 */
public class UsuarioDAOMySQL extends AccesoDatosMySQL implements UsuarioDAO {

    /** También lo usa {@link KinesiologoDAOMySQL} dentro de su transacción. */
    static final String INSERTAR =
            "INSERT INTO usuario (nombre_usuario, hash_contrasena, rol, activo) VALUES (?, ?, ?, ?)";

    private static final String SELECCIONAR = "SELECT " + Mapeos.COLUMNAS_USUARIO + " FROM usuario u";
    private static final String ACTUALIZAR =
            "UPDATE usuario SET nombre_usuario = ?, hash_contrasena = ?, rol = ?, activo = ? WHERE id_usuario = ?";

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public UsuarioDAOMySQL(ConexionBD conexion) {
        super(conexion);
    }

    @Override
    public int insertar(Usuario usuario) throws PersistenciaException {
        exigirNueva(usuario);
        usuario.setId(insertar(INSERTAR, usuario.getNombreUsuario(), usuario.getHashContrasena(), usuario.getRol(),
                usuario.isActivo()));
        return usuario.getId();
    }

    @Override
    public void actualizar(Usuario usuario) throws PersistenciaException {
        exigirExistente(actualizar(ACTUALIZAR, usuario.getNombreUsuario(), usuario.getHashContrasena(),
                usuario.getRol(), usuario.isActivo(), usuario.getId()), usuario);
    }

    @Override
    public Optional<Usuario> buscarPorId(int id) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE u.id_usuario = ?", Mapeos::usuario, id);
    }

    @Override
    public List<Usuario> listarTodos() throws PersistenciaException {
        return consultar(SELECCIONAR + " ORDER BY u.id_usuario", Mapeos::usuario);
    }

    @Override
    public Optional<Usuario> buscarPorNombreUsuario(String nombreUsuario) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE u.nombre_usuario = ?", Mapeos::usuario,
                nombreUsuario == null ? "" : nombreUsuario.trim());
    }
}
