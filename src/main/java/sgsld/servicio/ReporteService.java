package sgsld.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.Rol;
import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.servicio.reporte.Reporte;
import sgsld.servicio.reporte.ReporteDuracionPromedio;
import sgsld.servicio.reporte.ReportePorCategoria;
import sgsld.servicio.reporte.ReportePorMes;
import sgsld.servicio.reporte.ReportePorTipo;
import sgsld.servicio.reporte.ReportePorZona;
import sgsld.servicio.reporte.ReporteTratamientosPorEstado;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.PersistenciaException;
import sgsld.util.SesionUsuario;
import sgsld.util.Validaciones;
import sgsld.util.ValidacionException;

/** Reportes estadísticos (CU-07). Rol: Coordinador. */
public class ReporteService {

    private final List<Reporte> reportes;
    private final Clock reloj;

    /**
     * Crea el servicio y la lista de reportes disponibles.
     *
     * @param datos DAO de reportes
     * @param reloj reloj que define la fecha actual
     */
    public ReporteService(ReporteDAO datos, Clock reloj) {
        this.reportes = List.of(
                new ReportePorCategoria(datos),
                new ReportePorZona(datos),
                new ReportePorTipo(datos),
                new ReportePorMes(datos),
                new ReporteDuracionPromedio(datos),
                new ReporteTratamientosPorEstado(datos));
        this.reloj = reloj;
    }

    /**
     * Devuelve los reportes disponibles: una lista polimórfica que la vista
     * recorre sin conocer las clases concretas.
     *
     * @return los reportes
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public List<Reporte> getReportesDisponibles() throws AccesoDenegadoException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        return reportes;
    }

    /**
     * Genera un reporte para el período indicado.
     *
     * @param reporte reporte elegido
     * @param desde   fecha inicial, inclusive
     * @param hasta   fecha final, inclusive, no anterior a {@code desde}
     * @return las filas del reporte; vacía si no hay datos (CU-07, 5a)
     * @throws AccesoDenegadoException si el rol no es Coordinador
     * @throws ValidacionException     si el período es inválido
     */
    public List<FilaReporteDTO> generar(Reporte reporte, LocalDate desde, LocalDate hasta)
            throws AccesoDenegadoException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        if (reporte.usaPeriodo()) {
            List<String> errores = new ArrayList<>();
            Validaciones.obligatorio(errores, desde, "desde");
            Validaciones.obligatorio(errores, hasta, "hasta");
            if (desde != null && hasta != null && desde.isAfter(hasta)) {
                errores.add("La fecha desde no puede ser posterior a la fecha hasta.");
            }
            ValidacionException.lanzarSiHayErrores(errores);
        }
        return reporte.generar(desde, hasta);
    }

    /**
     * Fecha actual según el reloj del servicio.
     *
     * @return la fecha de hoy
     */
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }
}
