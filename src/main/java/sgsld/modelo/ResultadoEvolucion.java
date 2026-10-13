package sgsld.modelo;

/** Resultado de una evolución del tratamiento (columna {@code evolucion.resultado}). */
public enum ResultadoEvolucion {
    FAVORABLE("Favorable"),
    ESTABLE("Estable"),
    DESFAVORABLE("Desfavorable");

    private final String etiqueta;

    ResultadoEvolucion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Devuelve el nombre legible del resultado.
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
