package sgsld.modelo;

/**
 * Estados del tratamiento (diagrama de estados de la Figura 10): nace
 * "En curso" y la única transición es a "Finalizado".
 */
public enum EstadoTratamiento {
    EN_CURSO("En curso"),
    FINALIZADO("Finalizado");

    private final String etiqueta;

    EstadoTratamiento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Devuelve el nombre legible del estado.
     *
     * @return la etiqueta para mostrar
     */
    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
