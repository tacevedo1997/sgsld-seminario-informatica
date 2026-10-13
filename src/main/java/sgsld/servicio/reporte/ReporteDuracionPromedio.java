package sgsld.servicio.reporte;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.util.Algoritmos;
import sgsld.util.Texto;

/**
 * Duración promedio de los tratamientos finalizados por tipo de lesión (RF11,
 * consulta C8). Considera los tratamientos con fecha de fin en el período; el
 * promedio de días se redondea a un decimal y el orden es por promedio
 * descendente.
 */
public class ReporteDuracionPromedio extends Reporte {

    private static final Comparator<FilaReporteDTO> POR_PROMEDIO_DESCENDENTE =
            Comparator.comparingDouble(FilaReporteDTO::promedio).reversed()
                    .thenComparing(FilaReporteDTO::etiqueta, Texto.ordenAlfabetico());

    /**
     * Crea el reporte.
     *
     * @param datos DAO de reportes
     */
    public ReporteDuracionPromedio(ReporteDAO datos) {
        super(datos);
    }

    @Override
    public String getTitulo() {
        return "Duración promedio de tratamientos finalizados por tipo de lesión";
    }

    @Override
    public List<String> getEncabezados() {
        return List.of("Tipo de lesión", "Tratamientos finalizados", "Duración promedio (días)");
    }

    @Override
    public List<FilaReporteDTO> generar(LocalDate desde, LocalDate hasta) {
        Map<String, Long> cantidades = new HashMap<>();
        Map<String, Long> diasTotales = new HashMap<>();
        for (Tratamiento tratamiento : getDatos().tratamientosFinalizadosEntre(desde, hasta)) {
            String tipo = tratamiento.getLesion().getTipo().getNombre();
            cantidades.merge(tipo, 1L, Long::sum);
            diasTotales.merge(tipo, tratamiento.getDuracionDias(), Long::sum);
        }
        List<FilaReporteDTO> filas = new ArrayList<>();
        for (Map.Entry<String, Long> grupo : cantidades.entrySet()) {
            long cantidad = grupo.getValue();
            filas.add(new FilaReporteDTO(grupo.getKey(), cantidad, promedio(diasTotales.get(grupo.getKey()), cantidad)));
        }
        Algoritmos.ordenarPorInsercion(filas, POR_PROMEDIO_DESCENDENTE);
        return filas;
    }

    /** Agrega la duración promedio con coma decimal, por ejemplo "31,5". */
    @Override
    public List<String> getCeldas(FilaReporteDTO fila) {
        return List.of(fila.etiqueta(), String.valueOf(fila.cantidad()),
                String.format(Texto.LOCALE, "%.1f", fila.promedio()));
    }

    /** Grafica el promedio de días, no la cantidad. */
    @Override
    public double getValorGrafico(FilaReporteDTO fila) {
        return fila.promedio();
    }

    /** Equivale a {@code ROUND(AVG(dias), 1)}: redondeo a un decimal, mitad hacia arriba. */
    private static double promedio(long totalDias, long cantidad) {
        return BigDecimal.valueOf(totalDias)
                .divide(BigDecimal.valueOf(cantidad), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
