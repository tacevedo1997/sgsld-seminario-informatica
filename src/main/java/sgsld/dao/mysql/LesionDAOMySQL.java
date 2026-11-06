package sgsld.dao.mysql;

import java.util.List;
import java.util.Optional;

import sgsld.dao.LesionDAO;
import sgsld.modelo.Lesion;
import sgsld.util.PersistenciaException;

/**
 * Lesiones en MySQL (tabla {@code lesion}). Las lecturas devuelven el grafo
 * completo (jugador, tipo, zona, tratamientos y evoluciones) con
 * {@link CargadorGrafo}.
 */
public class LesionDAOMySQL extends AccesoDatosMySQL implements LesionDAO {

    private static final String INSERTAR = "INSERT INTO lesion (id_jugador, id_tipo, id_zona, gravedad, fecha, "
            + "descripcion) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String ACTUALIZAR = "UPDATE lesion SET id_jugador = ?, id_tipo = ?, id_zona = ?, "
            + "gravedad = ?, fecha = ?, descripcion = ? WHERE id_lesion = ?";

    private final CargadorGrafo grafo;

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public LesionDAOMySQL(ConexionBD conexion) {
        super(conexion);
        this.grafo = new CargadorGrafo(conexion);
    }

    @Override
    public int insertar(Lesion lesion) throws PersistenciaException {
        exigirNueva(lesion);
        lesion.setId(insertar(INSERTAR, lesion.getJugador().getId(), lesion.getTipo().getId(),
                lesion.getZona().getId(), lesion.getGravedad(), lesion.getFecha(), lesion.getDescripcion()));
        return lesion.getId();
    }

    @Override
    public void actualizar(Lesion lesion) throws PersistenciaException {
        exigirExistente(actualizar(ACTUALIZAR, lesion.getJugador().getId(), lesion.getTipo().getId(),
                lesion.getZona().getId(), lesion.getGravedad(), lesion.getFecha(), lesion.getDescripcion(),
                lesion.getId()), lesion);
    }

    @Override
    public Optional<Lesion> buscarPorId(int id) throws PersistenciaException {
        List<Lesion> lesiones = grafo.cargar(true, "l.id_lesion = ?", id);
        return lesiones.isEmpty() ? Optional.empty() : Optional.of(lesiones.get(0));
    }

    @Override
    public List<Lesion> listarTodos() throws PersistenciaException {
        return grafo.cargar(true, "TRUE");
    }

    @Override
    public List<Lesion> buscarPorJugador(int idJugador) throws PersistenciaException {
        return grafo.cargar(true, "l.id_jugador = ?", idJugador);
    }
}
