package sgsld.modelo;

/**
 * Raíz abstracta de todas las entidades persistentes del dominio.
 * <p>
 * Reemplaza los identificadores particulares del diseño ({@code idJugador},
 * {@code idLesion}, etc.) por un {@code id} común, que la capa de acceso a
 * datos asigna al insertar. Un {@code id} igual a 0 indica un objeto nuevo,
 * todavía no persistido.
 */
public abstract class Entidad {

    private int id;

    /** Crea una entidad nueva, sin identificador asignado. */
    protected Entidad() {
        this(0);
    }

    /**
     * Crea una entidad con un identificador conocido (carga de datos).
     *
     * @param id identificador, mayor o igual que 0
     */
    protected Entidad(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("El identificador no puede ser negativo");
        }
        this.id = id;
    }

    /**
     * Devuelve el identificador.
     *
     * @return el id, o 0 si la entidad no fue persistida
     */
    public int getId() {
        return id;
    }

    /**
     * Asigna el identificador. <b>Solo lo usa la capa DAO</b>, al insertar la
     * entidad (equivale al valor {@code AUTO_INCREMENT} de la base).
     *
     * @param id identificador asignado, mayor que 0
     */
    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El identificador asignado debe ser positivo");
        }
        this.id = id;
    }

    /**
     * Dos entidades son iguales si son de la misma clase y tienen el mismo id
     * distinto de 0. Una entidad nueva (id 0) solo es igual a sí misma.
     */
    @Override
    public final boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (otro == null || getClass() != otro.getClass()) {
            return false;
        }
        Entidad entidad = (Entidad) otro;
        return id != 0 && id == entidad.id;
    }

    /**
     * Coherente con {@link #equals(Object)}: usa la clase y el id, o la
     * identidad del objeto si el id es 0.
     */
    @Override
    public final int hashCode() {
        return id == 0 ? System.identityHashCode(this) : 31 * getClass().hashCode() + id;
    }
}
