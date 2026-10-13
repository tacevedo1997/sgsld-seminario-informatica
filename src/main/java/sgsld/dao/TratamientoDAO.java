package sgsld.dao;

import java.time.LocalDate;
import java.util.List;

import sgsld.modelo.Tratamiento;

/** Acceso a datos de tratamientos. */
public interface TratamientoDAO extends DAO<Tratamiento> {

    /**
     * Indica si la lesión tiene un tratamiento en curso (regla del índice
     * {@code uq_tratamiento_en_curso}).
     *
     * @param idLesion identificador de la lesión
     * @return {@code true} si existe un tratamiento en curso
     */
    boolean existeActivo(int idLesion);

    /**
     * Registra la finalización de un tratamiento (estado "Finalizado" y fecha
     * de fin).
     *
     * @param idTratamiento identificador del tratamiento
     * @param fechaFin      fecha de fin
     */
    void finalizar(int idTratamiento, LocalDate fechaFin);

    /**
     * Lista los tratamientos en curso.
     *
     * @return tratamientos con estado "En curso"
     */
    List<Tratamiento> listarEnCurso();
}
