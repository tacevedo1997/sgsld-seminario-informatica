package sgsld.dao.mysql;

import java.util.List;
import java.util.Optional;

import sgsld.dao.JugadorDAO;
import sgsld.modelo.Jugador;
import sgsld.util.PersistenciaException;

/**
 * Jugadores en MySQL (tabla {@code jugador}, con su categoría).
 * <p>
 * La búsqueda por DNI usa el índice único {@code uq_jugador_dni} de la base
 * (un árbol B, el equivalente de la búsqueda binaria del DAO en memoria). La
 * búsqueda por apellido es un {@code LIKE} parametrizado de coincidencia
 * parcial, que la intercalación {@code utf8mb4_0900_ai_ci} resuelve sin
 * distinguir mayúsculas ni tildes.
 */
public class JugadorDAOMySQL extends AccesoDatosMySQL implements JugadorDAO {

    private static final String SELECCIONAR = "SELECT " + Mapeos.COLUMNAS_JUGADOR + " FROM " + Mapeos.DESDE_JUGADOR;
    private static final String INSERTAR = "INSERT INTO jugador (dni, nombre, apellido, fecha_nacimiento, "
            + "numero_camiseta, posicion, activo, id_categoria) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String ACTUALIZAR = "UPDATE jugador SET dni = ?, nombre = ?, apellido = ?, "
            + "fecha_nacimiento = ?, numero_camiseta = ?, posicion = ?, activo = ?, id_categoria = ? "
            + "WHERE id_jugador = ?";

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public JugadorDAOMySQL(ConexionBD conexion) {
        super(conexion);
    }

    @Override
    public int insertar(Jugador jugador) throws PersistenciaException {
        exigirNueva(jugador);
        jugador.setId(insertar(INSERTAR, jugador.getDni(), jugador.getNombre(), jugador.getApellido(),
                jugador.getFechaNacimiento(), jugador.getNumeroCamiseta(), jugador.getPosicion(), jugador.isActivo(),
                jugador.getCategoria().getId()));
        return jugador.getId();
    }

    @Override
    public void actualizar(Jugador jugador) throws PersistenciaException {
        exigirExistente(actualizar(ACTUALIZAR, jugador.getDni(), jugador.getNombre(), jugador.getApellido(),
                jugador.getFechaNacimiento(), jugador.getNumeroCamiseta(), jugador.getPosicion(), jugador.isActivo(),
                jugador.getCategoria().getId(), jugador.getId()), jugador);
    }

    @Override
    public Optional<Jugador> buscarPorId(int id) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE j.id_jugador = ?", Mapeos::jugador, id);
    }

    @Override
    public List<Jugador> listarTodos() throws PersistenciaException {
        return consultar(SELECCIONAR + " ORDER BY j.id_jugador", Mapeos::jugador);
    }

    @Override
    public Optional<Jugador> buscarPorDni(String dni) throws PersistenciaException {
        if (dni == null) {
            return Optional.empty();
        }
        return consultarUno(SELECCIONAR + " WHERE j.dni = ?", Mapeos::jugador, dni.trim());
    }

    @Override
    public List<Jugador> buscarPorApellido(String apellido) throws PersistenciaException {
        return consultar(SELECCIONAR + " WHERE j.apellido LIKE ? ORDER BY j.id_jugador", Mapeos::jugador,
                patronContiene(apellido));
    }

    @Override
    public List<Jugador> listarActivos() throws PersistenciaException {
        return consultar(SELECCIONAR + " WHERE j.activo = TRUE ORDER BY j.id_jugador", Mapeos::jugador);
    }
}
