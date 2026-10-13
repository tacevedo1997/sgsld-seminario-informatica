package sgsld.servicio.reporte;

import java.time.LocalDate;
import java.util.List;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.dto.FilaReporteDTO;

/**
 * Reporte estadístico (CU-07). Raíz de una jerarquía que permite tratar todos
 * los reportes de manera uniforme: el servicio ofrece una lista de
 * {@code Reporte} y la vista los genera y muestra sin conocer su clase
 * concreta (polimorfismo).
 */
public abstract class Reporte {

    private final ReporteDAO datos;

    /**
     * Crea el reporte sobre el DAO de consultas.
     *
     * @param datos DAO de reportes
     */
    protected Reporte(ReporteDAO datos) {
        this.datos = datos;
    }

    /**
     * Título del reporte, para el menú y el encabezado de la tabla.
     *
     * @return el título
     */
    public abstract String getTitulo();

    /**
     * Encabezados de las columnas de la tabla.
     *
     * @return los encabezados
     */
    public abstract List<String> getEncabezados();

    /**
     * Indica si el reporte se filtra por un período. Por defecto, sí.
     *
     * @return {@code true} si usa las fechas desde y hasta
     */
    public boolean usaPeriodo() {
        return true;
    }

    /**
     * Indica si la tabla debe terminar con una fila de total. Por defecto, no.
     *
     * @return {@code true} en los reportes de conteo
     */
    public boolean muestraTotal() {
        return false;
    }

    /**
     * Genera las filas del reporte.
     *
     * @param desde fecha inicial del período, inclusive
     * @param hasta fecha final del período, inclusive
     * @return las filas, ya ordenadas; vacía si no hay datos
     */
    public abstract List<FilaReporteDTO> generar(LocalDate desde, LocalDate hasta);

    /**
     * Convierte una fila en las celdas de la tabla, en el orden de
     * {@link #getEncabezados()}. Por defecto: etiqueta y cantidad.
     *
     * @param fila fila del reporte
     * @return los textos de las celdas
     */
    public List<String> getCeldas(FilaReporteDTO fila) {
        return List.of(fila.etiqueta(), String.valueOf(fila.cantidad()));
    }

    /**
     * Valor que representa cada fila en el gráfico de barras. Por defecto, la
     * cantidad.
     *
     * @param fila fila del reporte
     * @return el valor a graficar
     */
    public double getValorGrafico(FilaReporteDTO fila) {
        return fila.cantidad();
    }

    /**
     * Da acceso a las consultas a las subclases.
     *
     * @return el DAO de reportes
     */
    protected ReporteDAO getDatos() {
        return datos;
    }

    @Override
    public String toString() {
        return getTitulo();
    }
}
