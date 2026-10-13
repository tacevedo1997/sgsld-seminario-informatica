package sgsld.modelo;

/** Roles de usuario del sistema; determinan los menús y operaciones permitidas. */
public enum Rol {
    COORDINADOR("Coordinador"),
    KINESIOLOGO("Kinesiólogo"),
    CUERPO_TECNICO("Cuerpo Técnico");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    /**
     * Devuelve el nombre legible del rol.
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
