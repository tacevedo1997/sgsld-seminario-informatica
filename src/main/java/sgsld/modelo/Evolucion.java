package sgsld.modelo;

import java.time.LocalDate;

/**
 * Registro de la evolución de un tratamiento en una fecha. No referencia a su
 * tratamiento: el tratamiento la contiene, y la capa DAO guarda la relación.
 */
public class Evolucion extends Entidad {

    /** Longitud máxima de la observación ({@code VARCHAR(500)}). */
    public static final int MAX_OBSERVACION = 500;

    private final LocalDate fecha;
    private final String observacion;
    private final ResultadoEvolucion resultado;

    /**
     * Crea una evolución nueva.
     *
     * @param fecha       fecha de la evolución
     * @param observacion observación obligatoria (hasta 500 caracteres)
     * @param resultado   resultado observado
     */
    public Evolucion(LocalDate fecha, String observacion, ResultadoEvolucion resultado) {
        this(0, fecha, observacion, resultado);
    }

    /**
     * Crea una evolución con identificador conocido (carga de datos).
     *
     * @param id          identificador
     * @param fecha       fecha de la evolución
     * @param observacion observación obligatoria (hasta 500 caracteres)
     * @param resultado   resultado observado
     */
    public Evolucion(int id, LocalDate fecha, String observacion, ResultadoEvolucion resultado) {
        super(id);
        this.fecha = fecha;
        this.observacion = observacion;
        this.resultado = resultado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getObservacion() {
        return observacion;
    }

    public ResultadoEvolucion getResultado() {
        return resultado;
    }

    @Override
    public String toString() {
        return resultado + ": " + observacion;
    }
}
