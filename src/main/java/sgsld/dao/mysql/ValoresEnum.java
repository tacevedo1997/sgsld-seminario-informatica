package sgsld.dao.mysql;

import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.EstadoTratamiento;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Region;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.Rol;

/**
 * Traducción entre las enumeraciones del modelo y las columnas {@code ENUM}
 * de la base. {@code usuario.rol} guarda el nombre de la constante
 * ({@code 'KINESIOLOGO'}); las demás columnas guardan la etiqueta legible
 * ({@code 'Leve'}, {@code 'En curso'}, {@code 'Miembro inferior'}), que es lo
 * que devuelve {@code toString()} de cada enumeración. Así el modelo no cambia.
 */
final class ValoresEnum {

    private ValoresEnum() {
    }

    /**
     * Valor que se guarda en la columna {@code ENUM}.
     *
     * @param valor constante de una enumeración del modelo
     * @return el nombre (en {@link Rol}) o la etiqueta (en las demás)
     */
    static String aColumna(Enum<?> valor) {
        return valor instanceof Rol ? valor.name() : valor.toString();
    }

    static Rol rol(String columna) {
        return Rol.valueOf(columna);
    }

    static Gravedad gravedad(String columna) {
        return porEtiqueta(Gravedad.class, columna);
    }

    static EstadoTratamiento estadoTratamiento(String columna) {
        return porEtiqueta(EstadoTratamiento.class, columna);
    }

    static ResultadoEvolucion resultado(String columna) {
        return porEtiqueta(ResultadoEvolucion.class, columna);
    }

    static Region region(String columna) {
        return porEtiqueta(Region.class, columna);
    }

    static EstadoDisponibilidad estadoDisponibilidad(String columna) {
        return porEtiqueta(EstadoDisponibilidad.class, columna);
    }

    /** Búsqueda secuencial de la constante cuya etiqueta coincide con el valor de la columna. */
    private static <E extends Enum<E>> E porEtiqueta(Class<E> tipo, String columna) {
        for (E constante : tipo.getEnumConstants()) {
            if (constante.toString().equals(columna)) {
                return constante;
            }
        }
        throw new IllegalArgumentException("Valor desconocido para " + tipo.getSimpleName() + ": " + columna);
    }
}
