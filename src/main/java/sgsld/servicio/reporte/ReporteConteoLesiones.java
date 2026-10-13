package sgsld.servicio.reporte;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.Lesion;
import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.util.Algoritmos;
import sgsld.util.Texto;

/**
 * Reporte que cuenta las lesiones del período agrupadas por un criterio.
 * <p>
 * {@link #generar(LocalDate, LocalDate)} es un <b>método plantilla</b>: filtra
 * las lesiones, las agrupa con {@link #clasificar(Lesion)} (definido por cada
 * subclase), cuenta y ordena con {@link #ordenar(List)}, que las subclases
 * pueden redefinir. Equivale a las consultas C4 a C7 de la base.
 */
public abstract class ReporteConteoLesiones extends Reporte {

    /** Orden por defecto: cantidad descendente y etiqueta ascendente. */
    private static final Comparator<FilaReporteDTO> POR_CANTIDAD_Y_ETIQUETA =
            Comparator.comparingLong(FilaReporteDTO::cantidad).reversed()
                    .thenComparing(FilaReporteDTO::etiqueta, Texto.ordenAlfabetico());

    /**
     * Crea el reporte.
     *
     * @param datos DAO de reportes
     */
    protected ReporteConteoLesiones(ReporteDAO datos) {
        super(datos);
    }

    /**
     * Nombre de la columna del criterio de agrupamiento.
     *
     * @return por ejemplo, "Categoría"
     */
    protected abstract String getNombreGrupo();

    /**
     * Paso variable: indica a qué grupo pertenece una lesión.
     *
     * @param lesion lesión del período
     * @return la etiqueta del grupo
     */
    protected abstract String clasificar(Lesion lesion);

    @Override
    public List<String> getEncabezados() {
        return List.of(getNombreGrupo(), "Lesiones");
    }

    @Override
    public boolean muestraTotal() {
        return true;
    }

    @Override
    public final List<FilaReporteDTO> generar(LocalDate desde, LocalDate hasta) {
        Map<String, Long> conteo = new HashMap<>();
        for (Lesion lesion : getDatos().lesionesEntre(desde, hasta)) {
            conteo.merge(clasificar(lesion), 1L, Long::sum);
        }
        List<FilaReporteDTO> filas = new ArrayList<>();
        for (Map.Entry<String, Long> grupo : conteo.entrySet()) {
            filas.add(FilaReporteDTO.conteo(grupo.getKey(), grupo.getValue()));
        }
        ordenar(filas);
        return filas;
    }

    /**
     * Ordena las filas por cantidad descendente y etiqueta ascendente (como
     * {@code ORDER BY lesiones DESC, etiqueta}). Las subclases pueden redefinirlo.
     *
     * @param filas filas a ordenar en el lugar
     */
    protected void ordenar(List<FilaReporteDTO> filas) {
        Algoritmos.ordenarPorInsercion(filas, POR_CANTIDAD_Y_ETIQUETA);
    }
}
