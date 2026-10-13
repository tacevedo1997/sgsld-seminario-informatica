package sgsld.modelo;

/** Categoría del club (Primera, Reserva, Sub-17...) con su rango etario. */
public class Categoria extends Entidad {

    /** Longitud máxima del nombre ({@code VARCHAR(40)}). */
    public static final int MAX_NOMBRE = 40;
    /** Edad mínima admitida en un rango ({@code TINYINT UNSIGNED}, acotado por el negocio). */
    public static final int EDAD_MINIMA = 0;
    /** Edad máxima admitida en un rango. */
    public static final int EDAD_MAXIMA = 99;

    private String nombre;
    private int edadMin;
    private int edadMax;

    /**
     * Crea una categoría nueva.
     *
     * @param nombre  nombre único de la categoría
     * @param edadMin edad mínima, inclusive
     * @param edadMax edad máxima, inclusive
     */
    public Categoria(String nombre, int edadMin, int edadMax) {
        this(0, nombre, edadMin, edadMax);
    }

    /**
     * Crea una categoría con identificador conocido (carga de datos).
     *
     * @param id      identificador
     * @param nombre  nombre único de la categoría
     * @param edadMin edad mínima, inclusive
     * @param edadMax edad máxima, inclusive
     */
    public Categoria(int id, String nombre, int edadMin, int edadMax) {
        super(id);
        this.nombre = nombre;
        this.edadMin = edadMin;
        this.edadMax = edadMax;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getEdadMin() {
        return edadMin;
    }

    public void setEdadMin(int edadMin) {
        this.edadMin = edadMin;
    }

    public int getEdadMax() {
        return edadMax;
    }

    public void setEdadMax(int edadMax) {
        this.edadMax = edadMax;
    }

    /**
     * Indica si una edad está dentro del rango de la categoría.
     *
     * @param edad edad en años cumplidos
     * @return {@code true} si {@code edadMin <= edad <= edadMax}
     */
    public boolean admiteEdad(int edad) {
        return edad >= edadMin && edad <= edadMax;
    }

    /**
     * Devuelve el rango etario para mostrar.
     *
     * @return por ejemplo, "18 a 40 años"
     */
    public String getRangoEtario() {
        return edadMin + " a " + edadMax + " años";
    }

    @Override
    public String toString() {
        return nombre;
    }
}
