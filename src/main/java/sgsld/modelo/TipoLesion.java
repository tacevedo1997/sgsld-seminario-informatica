package sgsld.modelo;

/** Tipo de lesión (muscular, ligamentaria...); catálogo. */
public class TipoLesion extends Entidad {

    private final String nombre;

    /**
     * Crea un tipo nuevo.
     *
     * @param nombre nombre del tipo
     */
    public TipoLesion(String nombre) {
        this(0, nombre);
    }

    /**
     * Crea un tipo con identificador conocido (carga de datos).
     *
     * @param id     identificador
     * @param nombre nombre del tipo
     */
    public TipoLesion(int id, String nombre) {
        super(id);
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
