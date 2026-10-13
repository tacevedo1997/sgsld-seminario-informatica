package sgsld.dao;

import java.time.LocalDate;
import java.util.List;

import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;

/** Consultas de apoyo a los reportes estadísticos (CU-07). */
public interface ReporteDAO {

    /**
     * Lista las lesiones ocurridas en el período, extremos incluidos.
     *
     * @param desde fecha inicial
     * @param hasta fecha final
     * @return lesiones con fecha entre {@code desde} y {@code hasta}
     */
    List<Lesion> lesionesEntre(LocalDate desde, LocalDate hasta);

    /**
     * Lista los tratamientos finalizados con fecha de fin en el período,
     * extremos incluidos.
     *
     * @param desde fecha inicial
     * @param hasta fecha final
     * @return tratamientos finalizados en el período
     */
    List<Tratamiento> tratamientosFinalizadosEntre(LocalDate desde, LocalDate hasta);

    /**
     * Lista todos los tratamientos, en curso y finalizados.
     *
     * @return todos los tratamientos
     */
    List<Tratamiento> listarTratamientos();
}
