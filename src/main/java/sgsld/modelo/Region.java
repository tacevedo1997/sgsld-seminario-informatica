package sgsld.modelo;

/**
 * Región del cuerpo a la que pertenece una zona corporal.
 * <p>
 * En la Figura 8 era un {@code String}; se alinea con el {@code ENUM} de la
 * columna {@code zona_corporal.region}.
 */
public enum Region {
    MIEMBRO_INFERIOR("Miembro inferior"),
    MIEMBRO_SUPERIOR("Miembro superior"),
    TRONCO("Tronco"),
    CABEZA_Y_CUELLO("Cabeza y cuello");

    private final String etiqueta;

    Region(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Devuelve el nombre legible de la región.
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
