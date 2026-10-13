package sgsld.modelo.dto;

/**
 * Fila de un reporte estadístico.
 *
 * @param etiqueta valor agrupado (categoría, zona, mes...)
 * @param cantidad cantidad de casos
 * @param promedio promedio de días; {@code null} en los reportes de conteo
 */
public record FilaReporteDTO(String etiqueta, long cantidad, Double promedio) {

    /**
     * Crea una fila de conteo, sin promedio.
     *
     * @param etiqueta valor agrupado
     * @param cantidad cantidad de casos
     * @return la fila
     */
    public static FilaReporteDTO conteo(String etiqueta, long cantidad) {
        return new FilaReporteDTO(etiqueta, cantidad, null);
    }
}
