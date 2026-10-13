package sgsld.modelo;

/** Gravedad de una lesión (columna {@code lesion.gravedad}). */
public enum Gravedad {
    LEVE("Leve"),
    MODERADA("Moderada"),
    GRAVE("Grave");

    private final String etiqueta;

    Gravedad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Devuelve el nombre legible de la gravedad.
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
