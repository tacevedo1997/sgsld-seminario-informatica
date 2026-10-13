package sgsld.modelo;

/**
 * Estado de disponibilidad de un jugador. No se almacena: se deriva de la
 * existencia de tratamientos en curso.
 */
public enum EstadoDisponibilidad {
    DISPONIBLE("Disponible"),
    EN_TRATAMIENTO("En tratamiento");

    private final String etiqueta;

    EstadoDisponibilidad(String etiqueta) {
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
