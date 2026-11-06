package sgsld.dao;

import java.time.LocalDate;
import java.util.List;

import sgsld.modelo.Evolucion;
import sgsld.modelo.Tratamiento;
import sgsld.util.PersistenciaException;

/** Acceso a datos de tratamientos. */
public interface TratamientoDAO extends DAO<Tratamiento> {

    /**
     * Indica si la lesión tiene un tratamiento en curso (regla del índice
     * {@code uq_tratamiento_en_curso}).
     *
     * @param idLesion identificador de la lesión
     * @return {@code true} si existe un tratamiento en curso
     */
    boolean existeActivo(int idLesion) throws PersistenciaException;

    /**
     * Registra la finalización de un tratamiento (estado "Finalizado" y fecha
     * de fin).
     *
     * @param idTratamiento identificador del tratamiento
     * @param fechaFin      fecha de fin
     */
    void finalizar(int idTratamiento, LocalDate fechaFin) throws PersistenciaException;

    /**
     * Registra la última evolución de un tratamiento y lo finaliza en una sola
     * transacción: se guardan las dos cosas o ninguna. Asigna el identificador
     * de la evolución.
     *
     * @param idTratamiento identificador del tratamiento en curso
     * @param evolucion     evolución nueva
     * @param fechaFin      fecha de fin del tratamiento
     * @return el identificador asignado a la evolución
     */
    int registrarEvolucionYFinalizar(int idTratamiento, Evolucion evolucion, LocalDate fechaFin)
            throws PersistenciaException;

    /**
     * Lista los tratamientos en curso.
     *
     * @return tratamientos con estado "En curso"
     */
    List<Tratamiento> listarEnCurso() throws PersistenciaException;
}
