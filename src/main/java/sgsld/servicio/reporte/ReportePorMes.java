package sgsld.servicio.reporte;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.Lesion;
import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.util.Algoritmos;

/**
 * Lesiones por mes (consulta C7). La clave tiene el formato {@code aaaa-MM}
 * y solo aparecen los meses con datos.
 */
public class ReportePorMes extends ReporteConteoLesiones {

    /**
     * Crea el reporte.
     *
     * @param datos DAO de reportes
     */
    public ReportePorMes(ReporteDAO datos) {
        super(datos);
    }

    @Override
    public String getTitulo() {
        return "Lesiones por mes";
    }

    @Override
    protected String getNombreGrupo() {
        return "Mes";
    }

    /** Clave {@code aaaa-MM}, como {@code DATE_FORMAT(fecha, '%Y-%m')}. */
    @Override
    protected String clasificar(Lesion lesion) {
        return YearMonth.from(lesion.getFecha()).toString();
    }

    /** Redefine el orden: cronológico por clave ascendente ({@code ORDER BY mes}). */
    @Override
    protected void ordenar(List<FilaReporteDTO> filas) {
        Algoritmos.ordenarPorInsercion(filas, Comparator.comparing(FilaReporteDTO::etiqueta));
    }
}
