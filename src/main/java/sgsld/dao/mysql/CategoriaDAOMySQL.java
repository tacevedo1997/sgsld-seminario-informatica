package sgsld.dao.mysql;

import java.util.List;
import java.util.Optional;

import sgsld.dao.CategoriaDAO;
import sgsld.modelo.Categoria;
import sgsld.util.PersistenciaException;

/** Categorías en MySQL (tabla {@code categoria}). */
public class CategoriaDAOMySQL extends AccesoDatosMySQL implements CategoriaDAO {

    private static final String SELECCIONAR = "SELECT " + Mapeos.COLUMNAS_CATEGORIA + " FROM categoria c";
    private static final String INSERTAR = "INSERT INTO categoria (nombre, edad_min, edad_max) VALUES (?, ?, ?)";
    private static final String ACTUALIZAR =
            "UPDATE categoria SET nombre = ?, edad_min = ?, edad_max = ? WHERE id_categoria = ?";

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public CategoriaDAOMySQL(ConexionBD conexion) {
        super(conexion);
    }

    @Override
    public int insertar(Categoria categoria) throws PersistenciaException {
        exigirNueva(categoria);
        categoria.setId(insertar(INSERTAR, categoria.getNombre(), categoria.getEdadMin(), categoria.getEdadMax()));
        return categoria.getId();
    }

    @Override
    public void actualizar(Categoria categoria) throws PersistenciaException {
        exigirExistente(actualizar(ACTUALIZAR, categoria.getNombre(), categoria.getEdadMin(), categoria.getEdadMax(),
                categoria.getId()), categoria);
    }

    @Override
    public Optional<Categoria> buscarPorId(int id) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE c.id_categoria = ?", Mapeos::categoria, id);
    }

    @Override
    public List<Categoria> listarTodos() throws PersistenciaException {
        return consultar(SELECCIONAR + " ORDER BY c.id_categoria", Mapeos::categoria);
    }

    /** La intercalación {@code utf8mb4_0900_ai_ci} no distingue mayúsculas ni tildes. */
    @Override
    public Optional<Categoria> buscarPorNombre(String nombre) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE c.nombre = ?", Mapeos::categoria, nombre == null ? "" : nombre.trim());
    }
}
