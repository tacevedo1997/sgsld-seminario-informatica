package sgsld.dao;

import java.util.List;
import java.util.Optional;

import sgsld.modelo.Entidad;

/**
 * Operaciones básicas de acceso a datos de una entidad (patrón DAO).
 * <p>
 * Abstrae el almacenamiento: en este incremento las implementaciones guardan
 * en memoria; en la entrega final, en MySQL mediante JDBC, sin cambios en el
 * modelo ni en los servicios.
 *
 * @param <T> tipo de entidad
 */
public interface DAO<T extends Entidad> {

    /**
     * Guarda una entidad nueva y le asigna su identificador.
     *
     * @param entidad entidad a guardar
     * @return el identificador asignado
     */
    int insertar(T entidad);

    /**
     * Guarda los cambios de una entidad existente.
     *
     * @param entidad entidad modificada
     */
    void actualizar(T entidad);

    /**
     * Busca una entidad por su identificador.
     *
     * @param id identificador
     * @return la entidad, o vacío si no existe
     */
    Optional<T> buscarPorId(int id);

    /**
     * Lista todas las entidades, en orden de inserción.
     *
     * @return una lista nueva (modificable por el llamador)
     */
    List<T> listarTodos();
}
