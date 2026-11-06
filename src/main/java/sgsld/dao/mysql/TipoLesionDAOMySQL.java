package sgsld.dao.mysql;

import java.util.List;
import java.util.Optional;

import sgsld.dao.TipoLesionDAO;
import sgsld.modelo.TipoLesion;
import sgsld.util.PersistenciaException;

/** Catálogo de tipos de lesión en MySQL (tabla {@code tipo_lesion}). */
public class TipoLesionDAOMySQL extends AccesoDatosMySQL implements TipoLesionDAO {

    private static final String SELECCIONAR = "SELECT " + Mapeos.COLUMNAS_TIPO + " FROM tipo_lesion tl";
    private static final String INSERTAR = "INSERT INTO tipo_lesion (nombre) VALUES (?)";
    private static final String ACTUALIZAR = "UPDATE tipo_lesion SET nombre = ? WHERE id_tipo = ?";

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public TipoLesionDAOMySQL(ConexionBD conexion) {
        super(conexion);
    }

    @Override
    public int insertar(TipoLesion tipo) throws PersistenciaException {
        exigirNueva(tipo);
        tipo.setId(insertar(INSERTAR, tipo.getNombre()));
        return tipo.getId();
    }

    @Override
    public void actualizar(TipoLesion tipo) throws PersistenciaException {
        exigirExistente(actualizar(ACTUALIZAR, tipo.getNombre(), tipo.getId()), tipo);
    }

    @Override
    public Optional<TipoLesion> buscarPorId(int id) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE tl.id_tipo = ?", Mapeos::tipo, id);
    }

    @Override
    public List<TipoLesion> listarTodos() throws PersistenciaException {
        return consultar(SELECCIONAR + " ORDER BY tl.id_tipo", Mapeos::tipo);
    }
}
