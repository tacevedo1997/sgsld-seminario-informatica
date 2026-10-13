package sgsld.servicio.reporte;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.EstadoTratamiento;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.dto.FilaReporteDTO;

/**
 * Tratamientos en curso y finalizados (consulta C9). Es una foto del momento:
 * no usa período. Ordena por estado (En curso, Finalizado) y solo incluye los
 * estados con tratamientos.
 */
public class ReporteTratamientosPorEstado extends Reporte {

    /**
     * Crea el reporte.
     *
     * @param datos DAO de reportes
     */
    public ReporteTratamientosPorEstado(ReporteDAO datos) {
        super(datos);
    }

    @Override
    public String getTitulo() {
        return "Tratamientos en curso y finalizados";
    }

    @Override
    public List<String> getEncabezados() {
        return List.of("Estado", "Tratamientos");
    }

    /** Redefine el valor por defecto: es una foto actual, sin período. */
    @Override
    public boolean usaPeriodo() {
        return false;
    }

    @Override
    public boolean muestraTotal() {
        return true;
    }

    /** Ignora las fechas: cuenta todos los tratamientos según su estado actual. */
    @Override
    public List<FilaReporteDTO> generar(LocalDate desde, LocalDate hasta) {
        List<Tratamiento> tratamientos = getDatos().listarTratamientos();
        List<FilaReporteDTO> filas = new ArrayList<>();
        for (EstadoTratamiento estado : EstadoTratamiento.values()) {
            long cantidad = 0;
            for (Tratamiento tratamiento : tratamientos) {
                if (tratamiento.getEstado() == estado) {
                    cantidad++;
                }
            }
            if (cantidad > 0) {
                filas.add(FilaReporteDTO.conteo(estado.getEtiqueta(), cantidad));
            }
        }
        return filas;
    }
}
