package sgsld.servicio.reporte;

import java.time.LocalDate;
import java.util.List;

import sgsld.dao.ReporteDAO;
import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.util.PersistenciaException;

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
     * @throws PersistenciaException si falla la lectura de los datos
     */
    public abstract List<FilaReporteDTO> generar(LocalDate desde, LocalDate hasta) throws PersistenciaException;

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
     * Convierte las filas generadas en una matriz de textos (filas × columnas):
     * la primera fila son los encabezados, luego una por fila del reporte con
     * sus celdas y, si el reporte lo indica, la fila "Total" con la suma de las
     * cantidades. La cantidad de filas y columnas se conoce al crearla, por eso
     * es un arreglo bidimensional; la usan la tabla de la interfaz y la
     * exportación a CSV.
     *
     * @param filas filas generadas por {@link #generar(LocalDate, LocalDate)}
     * @return la matriz, con al menos la fila de encabezados
     */
    public String[][] aMatriz(List<FilaReporteDTO> filas) {
        List<String> encabezados = getEncabezados();
        int columnas = encabezados.size();
        String[][] matriz = new String[1 + filas.size() + (muestraTotal() ? 1 : 0)][columnas];
        for (int columna = 0; columna < columnas; columna++) {
            matriz[0][columna] = encabezados.get(columna);
        }
        long total = 0;
        for (int fila = 0; fila < filas.size(); fila++) {
            List<String> celdas = getCeldas(filas.get(fila));
            for (int columna = 0; columna < columnas; columna++) {
                matriz[fila + 1][columna] = celdas.get(columna);
            }
            total += filas.get(fila).cantidad();
        }
        if (muestraTotal()) {
            String[] filaTotal = matriz[matriz.length - 1];
            filaTotal[0] = "Total";
            filaTotal[1] = String.valueOf(total);
            for (int columna = 2; columna < columnas; columna++) {
                filaTotal[columna] = "";
            }
        }
        return matriz;
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
