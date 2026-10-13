package sgsld.servicio.reporte;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.Lesion;

/** Lesiones por tipo de lesión (consulta C6). */
public class ReportePorTipo extends ReporteConteoLesiones {

    /**
     * Crea el reporte.
     *
     * @param datos DAO de reportes
     */
    public ReportePorTipo(ReporteDAO datos) {
        super(datos);
    }

    @Override
    public String getTitulo() {
        return "Lesiones por tipo";
    }

    @Override
    protected String getNombreGrupo() {
        return "Tipo de lesión";
    }

    @Override
    protected String clasificar(Lesion lesion) {
        return lesion.getTipo().getNombre();
    }
}
