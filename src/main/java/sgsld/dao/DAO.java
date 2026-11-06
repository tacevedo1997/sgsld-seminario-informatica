package sgsld.dao;

import java.util.List;
import java.util.Optional;

import sgsld.modelo.Entidad;
import sgsld.util.PersistenciaException;

/**
 * Operaciones básicas de acceso a datos de una entidad (patrón DAO).
 * <p>
 * Abstrae el almacenamiento: hay implementaciones en memoria
 * ({@code dao.memoria}) y en MySQL con JDBC ({@code dao.mysql}). Todas las
 * operaciones declaran {@link PersistenciaException}, que las
 * implementaciones en memoria nunca lanzan.
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
    int insertar(T entidad) throws PersistenciaException;

    /**
     * Guarda los cambios de una entidad existente.
     *
     * @param entidad entidad modificada
     */
    void actualizar(T entidad) throws PersistenciaException;

    /**
     * Busca una entidad por su identificador.
     *
     * @param id identificador
     * @return la entidad, o vacío si no existe
     */
    Optional<T> buscarPorId(int id) throws PersistenciaException;

    /**
     * Lista todas las entidades, en orden de inserción.
     *
     * @return una lista nueva (modificable por el llamador)
     */
    List<T> listarTodos() throws PersistenciaException;
}
