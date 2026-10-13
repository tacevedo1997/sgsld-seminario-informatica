package sgsld.modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import sgsld.util.Algoritmos;

/**
 * Tratamiento indicado por un kinesiólogo para una lesión.
 * <p>
 * <b>Encapsula el diagrama de estados de la Figura 10</b>: no hay
 * {@code setEstado} ni {@code setFechaFin}. Todo tratamiento nace "En curso" y
 * la única transición es {@link #finalizar(LocalDate)}. Las evoluciones solo
 * se registran mientras está en curso.
 */
public class Tratamiento extends Entidad {

    /** Longitud máxima del tipo de tratamiento ({@code VARCHAR(60)}). */
    public static final int MAX_TIPO = 60;
    /** Longitud máxima de las observaciones ({@code VARCHAR(500)}). */
    public static final int MAX_OBSERVACIONES = 500;

    private final Lesion lesion;
    private final Kinesiologo kinesiologo;
    private final LocalDate fechaInicio;
    private final String tipo;
    private final String observaciones;
    private final List<Evolucion> evoluciones = new ArrayList<>();
    private LocalDate fechaFin;
    private EstadoTratamiento estado;

    /**
     * Crea un tratamiento nuevo, en curso.
     *
     * @param lesion        lesión tratada
     * @param kinesiologo   kinesiólogo responsable
     * @param fechaInicio   fecha de inicio
     * @param tipo          tipo de tratamiento (hasta 60 caracteres)
     * @param observaciones observaciones opcionales (hasta 500 caracteres)
     */
    public Tratamiento(Lesion lesion, Kinesiologo kinesiologo, LocalDate fechaInicio,
                       String tipo, String observaciones) {
        this(0, lesion, kinesiologo, fechaInicio, tipo, observaciones);
    }

    /**
     * Crea un tratamiento en curso con identificador conocido (carga de
     * datos). Un tratamiento finalizado se reconstruye llamando luego a
     * {@link #finalizar(LocalDate)}, de modo que también la carga respeta el
     * diagrama de estados.
     *
     * @param id            identificador
     * @param lesion        lesión tratada
     * @param kinesiologo   kinesiólogo responsable
     * @param fechaInicio   fecha de inicio
     * @param tipo          tipo de tratamiento (hasta 60 caracteres)
     * @param observaciones observaciones opcionales (hasta 500 caracteres)
     */
    public Tratamiento(int id, Lesion lesion, Kinesiologo kinesiologo, LocalDate fechaInicio,
                       String tipo, String observaciones) {
        super(id);
        this.lesion = lesion;
        this.kinesiologo = kinesiologo;
        this.fechaInicio = fechaInicio;
        this.tipo = tipo;
        this.observaciones = observaciones;
        this.estado = EstadoTratamiento.EN_CURSO;
        this.fechaFin = null;
    }

    public Lesion getLesion() {
        return lesion;
    }

    public Kinesiologo getKinesiologo() {
        return kinesiologo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    /**
     * Devuelve la fecha de fin.
     *
     * @return la fecha de fin, o {@code null} si sigue en curso
     */
    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public String getTipo() {
        return tipo;
    }

    public EstadoTratamiento getEstado() {
        return estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    /**
     * Indica si el tratamiento sigue en curso.
     *
     * @return {@code true} si su estado es "En curso"
     */
    public boolean estaEnCurso() {
        return estado == EstadoTratamiento.EN_CURSO;
    }

    /**
     * Finaliza el tratamiento: única transición del diagrama de estados.
     *
     * @param fechaFin fecha de fin, no anterior a la de inicio
     * @throws IllegalStateException    si el tratamiento ya estaba finalizado
     * @throws IllegalArgumentException si la fecha es nula o anterior al inicio
     */
    public void finalizar(LocalDate fechaFin) {
        if (!estaEnCurso()) {
            throw new IllegalStateException("El tratamiento ya está finalizado");
        }
        if (fechaFin == null) {
            throw new IllegalArgumentException("La fecha de fin es obligatoria");
        }
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }
        this.fechaFin = fechaFin;
        this.estado = EstadoTratamiento.FINALIZADO;
    }

    /**
     * Registra una evolución del tratamiento.
     *
     * @param evolucion evolución a agregar
     * @throws IllegalStateException si el tratamiento ya está finalizado
     */
    public void agregarEvolucion(Evolucion evolucion) {
        Objects.requireNonNull(evolucion, "evolucion");
        if (!estaEnCurso()) {
            throw new IllegalStateException("Solo se registran evoluciones en tratamientos en curso");
        }
        evoluciones.add(evolucion);
    }

    /**
     * Devuelve las evoluciones registradas.
     *
     * @return lista no modificable
     */
    public List<Evolucion> getEvoluciones() {
        return Collections.unmodifiableList(evoluciones);
    }

    /**
     * Devuelve la cantidad de evoluciones registradas.
     *
     * @return cantidad de evoluciones
     */
    public int getCantidadEvoluciones() {
        return evoluciones.size();
    }

    /** Ordena las evoluciones por fecha (ordenamiento por inserción estable). */
    public void ordenarEvoluciones() {
        Algoritmos.ordenarPorInsercion(evoluciones, Comparator.comparing(Evolucion::getFecha));
    }

    /**
     * Duración de un tratamiento finalizado: días entre el inicio y el fin,
     * como {@code DATEDIFF(fecha_fin, fecha_inicio)}.
     *
     * @return cantidad de días
     * @throws IllegalStateException si el tratamiento sigue en curso
     */
    public long getDuracionDias() {
        if (estaEnCurso()) {
            throw new IllegalStateException("Un tratamiento en curso no tiene duración final");
        }
        return ChronoUnit.DAYS.between(fechaInicio, fechaFin);
    }

    /**
     * Días transcurridos desde el inicio de un tratamiento en curso.
     *
     * @param hoy fecha actual
     * @return cantidad de días desde el inicio
     * @throws IllegalStateException si el tratamiento ya está finalizado
     */
    public long getDiasTranscurridos(LocalDate hoy) {
        if (!estaEnCurso()) {
            throw new IllegalStateException("El tratamiento está finalizado: use getDuracionDias()");
        }
        return ChronoUnit.DAYS.between(fechaInicio, hoy);
    }

    @Override
    public String toString() {
        return tipo + " (" + estado + ")";
    }
}
