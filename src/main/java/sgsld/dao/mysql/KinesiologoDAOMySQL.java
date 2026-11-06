package sgsld.dao.mysql;

import java.util.List;
import java.util.Optional;

import sgsld.dao.KinesiologoDAO;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Usuario;
import sgsld.util.PersistenciaException;

/** Kinesiólogos en MySQL (tabla {@code kinesiologo}, con su usuario opcional). */
public class KinesiologoDAOMySQL extends AccesoDatosMySQL implements KinesiologoDAO {

    private static final String SELECCIONAR =
            "SELECT " + Mapeos.COLUMNAS_KINESIOLOGO + " FROM " + Mapeos.DESDE_KINESIOLOGO;
    private static final String INSERTAR =
            "INSERT INTO kinesiologo (nombre, apellido, matricula, id_usuario) VALUES (?, ?, ?, ?)";
    private static final String ACTUALIZAR = "UPDATE kinesiologo SET nombre = ?, apellido = ?, matricula = ?, "
            + "id_usuario = ? WHERE id_kinesiologo = ?";

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public KinesiologoDAOMySQL(ConexionBD conexion) {
        super(conexion);
    }

    @Override
    public int insertar(Kinesiologo kinesiologo) throws PersistenciaException {
        exigirNueva(kinesiologo);
        kinesiologo.setId(insertar(INSERTAR, kinesiologo.getNombre(), kinesiologo.getApellido(),
                kinesiologo.getMatricula(), idUsuario(kinesiologo)));
        return kinesiologo.getId();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Las dos inserciones comparten una transacción: si falla la segunda (por
     * ejemplo, matrícula duplicada), la primera se deshace y el usuario no queda
     * creado. Los identificadores se asignan a los objetos recién después del
     * {@code commit}.
     */
    @Override
    public int insertarConUsuario(Kinesiologo kinesiologo, Usuario usuario) throws PersistenciaException {
        exigirNueva(kinesiologo);
        exigirNueva(usuario);
        int[] ids = enTransaccion("registrar el kinesiólogo y su usuario", conexion -> {
            int idUsuario = insertar(conexion, UsuarioDAOMySQL.INSERTAR, usuario.getNombreUsuario(),
                    usuario.getHashContrasena(), usuario.getRol(), usuario.isActivo());
            int idKinesiologo = insertar(conexion, INSERTAR, kinesiologo.getNombre(), kinesiologo.getApellido(),
                    kinesiologo.getMatricula(), idUsuario);
            return new int[] {idUsuario, idKinesiologo};
        });
        usuario.setId(ids[0]);
        kinesiologo.setUsuario(usuario);
        kinesiologo.setId(ids[1]);
        return kinesiologo.getId();
    }

    @Override
    public void actualizar(Kinesiologo kinesiologo) throws PersistenciaException {
        exigirExistente(actualizar(ACTUALIZAR, kinesiologo.getNombre(), kinesiologo.getApellido(),
                kinesiologo.getMatricula(), idUsuario(kinesiologo), kinesiologo.getId()), kinesiologo);
    }

    @Override
    public Optional<Kinesiologo> buscarPorId(int id) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE k.id_kinesiologo = ?", Mapeos::kinesiologo, id);
    }

    @Override
    public List<Kinesiologo> listarTodos() throws PersistenciaException {
        return consultar(SELECCIONAR + " ORDER BY k.id_kinesiologo", Mapeos::kinesiologo);
    }

    @Override
    public Optional<Kinesiologo> buscarPorMatricula(String matricula) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE k.matricula = ?", Mapeos::kinesiologo,
                matricula == null ? "" : matricula.trim());
    }

    @Override
    public Optional<Kinesiologo> buscarPorUsuario(int idUsuario) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE k.id_usuario = ?", Mapeos::kinesiologo, idUsuario);
    }

    private static Integer idUsuario(Kinesiologo kinesiologo) {
        return kinesiologo.tieneUsuario() ? kinesiologo.getUsuario().getId() : null;
    }
}
