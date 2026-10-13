package sgsld.modelo;

/** Zona corporal afectada por una lesión (catálogo). */
public class ZonaCorporal extends Entidad {

    private final String nombre;
    private final Region region;

    /**
     * Crea una zona nueva.
     *
     * @param nombre nombre de la zona
     * @param region región del cuerpo
     */
    public ZonaCorporal(String nombre, Region region) {
        this(0, nombre, region);
    }

    /**
     * Crea una zona con identificador conocido (carga de datos).
     *
     * @param id     identificador
     * @param nombre nombre de la zona
     * @param region región del cuerpo
     */
    public ZonaCorporal(int id, String nombre, Region region) {
        super(id);
        this.nombre = nombre;
        this.region = region;
    }

    public String getNombre() {
        return nombre;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
