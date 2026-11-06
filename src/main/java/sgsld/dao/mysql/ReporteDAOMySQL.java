package sgsld.dao.mysql;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.EstadoTratamiento;
import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;
import sgsld.util.PersistenciaException;

/**
 * Consultas de los reportes en MySQL. El filtro por período lo hace la base
 * ({@code BETWEEN}, extremos incluidos); el agrupamiento lo hacen los reportes
 * en Java (jerarquía {@code Reporte}), igual que con los DAO en memoria. Las
 * evoluciones no se cargan porque ningún reporte las usa.
 */
public class ReporteDAOMySQL extends AccesoDatosMySQL implements ReporteDAO {

    private static final String LESIONES_ENTRE = "l.fecha BETWEEN ? AND ?";
    private static final String CON_FINALIZADO_ENTRE = "l.id_lesion IN (SELECT t2.id_lesion FROM tratamiento t2 "
            + "WHERE t2.estado = ? AND t2.fecha_fin BETWEEN ? AND ?)";

    private final CargadorGrafo grafo;

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public ReporteDAOMySQL(ConexionBD conexion) {
        super(conexion);
        this.grafo = new CargadorGrafo(conexion);
    }

    @Override
    public List<Lesion> lesionesEntre(LocalDate desde, LocalDate hasta) throws PersistenciaException {
        return grafo.cargar(false, LESIONES_ENTRE, desde, hasta);
    }

    @Override
    public List<Tratamiento> tratamientosFinalizadosEntre(LocalDate desde, LocalDate hasta)
            throws PersistenciaException {
        List<Tratamiento> resultado = new ArrayList<>();
        for (Tratamiento tratamiento : CargadorGrafo.tratamientos(
                grafo.cargar(false, CON_FINALIZADO_ENTRE, EstadoTratamiento.FINALIZADO, desde, hasta))) {
            if (!tratamiento.estaEnCurso() && !tratamiento.getFechaFin().isBefore(desde)
                    && !tratamiento.getFechaFin().isAfter(hasta)) {
                resultado.add(tratamiento);
            }
        }
        return resultado;
    }

    @Override
    public List<Tratamiento> listarTratamientos() throws PersistenciaException {
        return CargadorGrafo.tratamientos(grafo.cargar(false, "TRUE"));
    }
}
