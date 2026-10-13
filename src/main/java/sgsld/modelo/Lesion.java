package sgsld.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import sgsld.util.Algoritmos;

/**
 * Lesión sufrida por un jugador. Agrupa los tratamientos indicados para ella.
 * Sus datos no cambian una vez registrada; solo se le agregan tratamientos.
 */
public class Lesion extends Entidad {

    /** Longitud máxima de la descripción ({@code VARCHAR(500)}). */
    public static final int MAX_DESCRIPCION = 500;

    private final Jugador jugador;
    private final TipoLesion tipo;
    private final ZonaCorporal zona;
    private final Gravedad gravedad;
    private final LocalDate fecha;
    private final String descripcion;
    private final List<Tratamiento> tratamientos = new ArrayList<>();

    /**
     * Crea una lesión nueva.
     *
     * @param jugador     jugador lesionado
     * @param tipo        tipo de lesión
     * @param zona        zona corporal afectada
     * @param gravedad    gravedad
     * @param fecha       fecha en que ocurrió
     * @param descripcion descripción opcional (hasta 500 caracteres)
     */
    public Lesion(Jugador jugador, TipoLesion tipo, ZonaCorporal zona, Gravedad gravedad,
                  LocalDate fecha, String descripcion) {
        this(0, jugador, tipo, zona, gravedad, fecha, descripcion);
    }

    /**
     * Crea una lesión con identificador conocido (carga de datos).
     *
     * @param id          identificador
     * @param jugador     jugador lesionado
     * @param tipo        tipo de lesión
     * @param zona        zona corporal afectada
     * @param gravedad    gravedad
     * @param fecha       fecha en que ocurrió
     * @param descripcion descripción opcional (hasta 500 caracteres)
     */
    public Lesion(int id, Jugador jugador, TipoLesion tipo, ZonaCorporal zona, Gravedad gravedad,
                  LocalDate fecha, String descripcion) {
        super(id);
        this.jugador = jugador;
        this.tipo = tipo;
        this.zona = zona;
        this.gravedad = gravedad;
        this.fecha = fecha;
        this.descripcion = descripcion;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public TipoLesion getTipo() {
        return tipo;
    }

    public ZonaCorporal getZona() {
        return zona;
    }

    public Gravedad getGravedad() {
        return gravedad;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Devuelve los tratamientos de la lesión.
     *
     * @return lista no modificable
     */
    public List<Tratamiento> getTratamientos() {
        return Collections.unmodifiableList(tratamientos);
    }

    /**
     * Asocia un tratamiento a la lesión. Protege la regla de la base
     * ({@code uq_tratamiento_en_curso}): no puede haber dos tratamientos en
     * curso para la misma lesión.
     *
     * @param tratamiento tratamiento cuya lesión es esta
     * @throws IllegalArgumentException si el tratamiento corresponde a otra lesión o ya está asociado
     * @throws IllegalStateException    si ya hay un tratamiento en curso
     */
    public void agregarTratamiento(Tratamiento tratamiento) {
        Objects.requireNonNull(tratamiento, "tratamiento");
        if (!equals(tratamiento.getLesion())) {
            throw new IllegalArgumentException("El tratamiento corresponde a otra lesión");
        }
        if (tratamientos.contains(tratamiento)) {
            throw new IllegalArgumentException("El tratamiento ya está asociado a la lesión");
        }
        if (tratamiento.estaEnCurso() && tieneTratamientoEnCurso()) {
            throw new IllegalStateException("La lesión ya tiene un tratamiento en curso");
        }
        tratamientos.add(tratamiento);
    }

    /**
     * Indica si alguno de sus tratamientos sigue en curso.
     *
     * @return {@code true} si hay un tratamiento en curso
     */
    public boolean tieneTratamientoEnCurso() {
        for (Tratamiento tratamiento : tratamientos) {
            if (tratamiento.estaEnCurso()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indica si la lesión afecta la zona indicada (compara por id).
     *
     * @param otra zona a comparar
     * @return {@code true} si es la misma zona corporal
     */
    public boolean esMismaZona(ZonaCorporal otra) {
        return zona != null && otra != null && zona.getId() == otra.getId();
    }

    /**
     * Devuelve el tratamiento con la fecha de inicio más reciente. Ante
     * fechas iguales, el último agregado.
     *
     * @return el último tratamiento, o {@code null} si la lesión no tiene
     */
    public Tratamiento getUltimoTratamiento() {
        Tratamiento ultimo = null;
        for (Tratamiento tratamiento : tratamientos) {
            if (ultimo == null || !tratamiento.getFechaInicio().isBefore(ultimo.getFechaInicio())) {
                ultimo = tratamiento;
            }
        }
        return ultimo;
    }

    /**
     * Ordena los tratamientos por fecha de inicio (ordenamiento por inserción
     * estable), y las evoluciones de cada uno por fecha.
     */
    public void ordenarTratamientos() {
        Algoritmos.ordenarPorInsercion(tratamientos, Comparator.comparing(Tratamiento::getFechaInicio));
        for (Tratamiento tratamiento : tratamientos) {
            tratamiento.ordenarEvoluciones();
        }
    }

    @Override
    public String toString() {
        return (zona == null ? "-" : zona.getNombre()) + " (" + (tipo == null ? "-" : tipo.getNombre()) + ")";
    }
}
