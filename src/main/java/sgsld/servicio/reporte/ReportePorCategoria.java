package sgsld.servicio.reporte;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.Lesion;

/** Lesiones por categoría vigente del jugador (consulta C4). */
public class ReportePorCategoria extends ReporteConteoLesiones {

    /**
     * Crea el reporte.
     *
     * @param datos DAO de reportes
     */
    public ReportePorCategoria(ReporteDAO datos) {
        super(datos);
    }

    @Override
    public String getTitulo() {
        return "Lesiones por categoría";
    }

    @Override
    protected String getNombreGrupo() {
        return "Categoría";
    }

    @Override
    protected String clasificar(Lesion lesion) {
        return lesion.getJugador().getCategoria().getNombre();
    }
}
