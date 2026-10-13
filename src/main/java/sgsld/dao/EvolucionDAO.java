package sgsld.dao;

import java.util.List;

import sgsld.modelo.Evolucion;

/**
 * Acceso a datos de evoluciones. No extiende {@link DAO} porque
 * {@code Evolucion} no referencia a su tratamiento: la relación se indica al
 * insertar.
 */
public interface EvolucionDAO {

    /**
     * Guarda una evolución nueva de un tratamiento y le asigna su identificador.
     *
     * @param idTratamiento identificador del tratamiento
     * @param evolucion     evolución a guardar
     * @return el identificador asignado
     */
    int insertar(int idTratamiento, Evolucion evolucion);

    /**
     * Lista las evoluciones de un tratamiento.
     *
     * @param idTratamiento identificador del tratamiento
     * @return las evoluciones, en orden de registro
     */
    List<Evolucion> buscarPorTratamiento(int idTratamiento);
}
