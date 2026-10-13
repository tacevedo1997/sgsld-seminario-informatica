package sgsld.dao.memoria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import sgsld.dao.LesionDAO;
import sgsld.dao.ReporteDAO;
import sgsld.dao.TratamientoDAO;
import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;

/** Consultas de los reportes, resueltas sobre las tablas en memoria. */
public class ReporteDAOMemoria implements ReporteDAO {

    private final LesionDAO lesiones;
    private final TratamientoDAO tratamientos;

    /**
     * Crea el DAO de reportes sobre las tablas de lesiones y tratamientos.
     *
     * @param lesiones     DAO de lesiones
     * @param tratamientos DAO de tratamientos
     */
    public ReporteDAOMemoria(LesionDAO lesiones, TratamientoDAO tratamientos) {
        this.lesiones = lesiones;
        this.tratamientos = tratamientos;
    }

    @Override
    public List<Lesion> lesionesEntre(LocalDate desde, LocalDate hasta) {
        List<Lesion> resultado = new ArrayList<>();
        for (Lesion lesion : lesiones.listarTodos()) {
            if (entre(lesion.getFecha(), desde, hasta)) {
                resultado.add(lesion);
            }
        }
        return resultado;
    }

    @Override
    public List<Tratamiento> tratamientosFinalizadosEntre(LocalDate desde, LocalDate hasta) {
        List<Tratamiento> resultado = new ArrayList<>();
        for (Tratamiento tratamiento : tratamientos.listarTodos()) {
            if (!tratamiento.estaEnCurso() && entre(tratamiento.getFechaFin(), desde, hasta)) {
                resultado.add(tratamiento);
            }
        }
        return resultado;
    }

    @Override
    public List<Tratamiento> listarTratamientos() {
        return tratamientos.listarTodos();
    }

    /** Equivale a {@code fecha BETWEEN desde AND hasta}: extremos incluidos. */
    private static boolean entre(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }
}
