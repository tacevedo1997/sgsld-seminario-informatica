package sgsld.controlador;

import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;
import sgsld.util.Fechas;

/** Textos para mostrar lesiones y tratamientos, compartidos por los controladores. */
public final class Formatos {

    private Formatos() {
    }

    /**
     * Describe una lesión en una línea.
     *
     * @param lesion lesión a describir
     * @return por ejemplo, "15/06/2026 - Isquiotibiales (Muscular) - Grave"
     */
    public static String lesion(Lesion lesion) {
        return Fechas.formatear(lesion.getFecha()) + " - " + lesion.getZona().getNombre() + " ("
                + lesion.getTipo().getNombre() + ") - " + lesion.getGravedad();
    }

    /**
     * Describe el estado de una lesión según su último tratamiento.
     *
     * @param lesion lesión a describir
     * @return "Tratamiento en curso", "Finalizado el dd/mm/aaaa" o "Sin tratamiento"
     */
    public static String estadoDeLesion(Lesion lesion) {
        Tratamiento ultimo = lesion.getUltimoTratamiento();
        if (ultimo == null) {
            return "Sin tratamiento";
        }
        return ultimo.estaEnCurso() ? "Tratamiento en curso" : "Finalizado el " + Fechas.formatear(ultimo.getFechaFin());
    }

    /**
     * Texto de una cantidad de días con singular y plural.
     *
     * @param dias cantidad de días
     * @return por ejemplo, "1 día" o "40 días"
     */
    public static String dias(long dias) {
        return dias == 1 ? "1 día" : dias + " días";
    }
}
