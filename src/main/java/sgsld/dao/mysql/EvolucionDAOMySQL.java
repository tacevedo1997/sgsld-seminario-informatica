package sgsld.dao.mysql;

import java.util.List;

import sgsld.dao.EvolucionDAO;
import sgsld.modelo.Evolucion;
import sgsld.util.PersistenciaException;

/** Evoluciones en MySQL (tabla {@code evolucion}). */
public class EvolucionDAOMySQL extends AccesoDatosMySQL implements EvolucionDAO {

    /** También lo usa {@link TratamientoDAOMySQL} dentro de su transacción. */
    static final String INSERTAR =
            "INSERT INTO evolucion (id_tratamiento, fecha, observacion, resultado) VALUES (?, ?, ?, ?)";

    private static final String DEL_TRATAMIENTO = "SELECT " + Mapeos.COLUMNAS_EVOLUCION
            + " FROM evolucion e WHERE e.id_tratamiento = ? ORDER BY e.fecha, e.id_evolucion";

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public EvolucionDAOMySQL(ConexionBD conexion) {
        super(conexion);
    }

    @Override
    public int insertar(int idTratamiento, Evolucion evolucion) throws PersistenciaException {
        exigirNueva(evolucion);
        evolucion.setId(insertar(INSERTAR, idTratamiento, evolucion.getFecha(), evolucion.getObservacion(),
                evolucion.getResultado()));
        return evolucion.getId();
    }

    @Override
    public List<Evolucion> buscarPorTratamiento(int idTratamiento) throws PersistenciaException {
        return consultar(DEL_TRATAMIENTO, Mapeos::evolucion, idTratamiento);
    }
}
