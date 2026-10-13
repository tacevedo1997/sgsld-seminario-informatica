package sgsld.modelo;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.regex.Pattern;

import sgsld.util.Fechas;
import sgsld.util.Texto;
import sgsld.util.Validaciones;

/**
 * Jugador del club. Pertenece a una categoría y acumula un historial de
 * lesiones. La baja es lógica ({@code activo = false}): conserva su historial.
 */
public class Jugador extends Persona {

    /** Longitud máxima de la posición ({@code VARCHAR(30)}). */
    public static final int MAX_POSICION = 30;
    /** Número de camiseta mínimo. */
    public static final int CAMISETA_MINIMA = 1;
    /** Número de camiseta máximo. */
    public static final int CAMISETA_MAXIMA = 99;

    private static final Pattern FORMATO_DNI = Pattern.compile("\\d{7,8}");

    private String dni;
    private LocalDate fechaNacimiento;
    private Integer numeroCamiseta;
    private String posicion;
    private boolean activo;
    private Categoria categoria;

    /**
     * Crea un jugador nuevo y activo.
     *
     * @param dni             DNI de 7 u 8 dígitos
     * @param nombre          nombre de pila
     * @param apellido        apellido
     * @param fechaNacimiento fecha de nacimiento
     * @param numeroCamiseta  número de camiseta (1 a 99) o {@code null}
     * @param posicion        posición de juego
     * @param categoria       categoría vigente
     */
    public Jugador(String dni, String nombre, String apellido, LocalDate fechaNacimiento,
                   Integer numeroCamiseta, String posicion, Categoria categoria) {
        this(0, dni, nombre, apellido, fechaNacimiento, numeroCamiseta, posicion, true, categoria);
    }

    /**
     * Crea un jugador con identificador conocido (carga de datos).
     *
     * @param id              identificador
     * @param dni             DNI de 7 u 8 dígitos
     * @param nombre          nombre de pila
     * @param apellido        apellido
     * @param fechaNacimiento fecha de nacimiento
     * @param numeroCamiseta  número de camiseta (1 a 99) o {@code null}
     * @param posicion        posición de juego
     * @param activo          {@code false} si fue dado de baja
     * @param categoria       categoría vigente
     */
    public Jugador(int id, String dni, String nombre, String apellido, LocalDate fechaNacimiento,
                   Integer numeroCamiseta, String posicion, boolean activo, Categoria categoria) {
        super(id, nombre, apellido);
        this.dni = dni;
        this.fechaNacimiento = fechaNacimiento;
        this.numeroCamiseta = numeroCamiseta;
        this.posicion = posicion;
        this.activo = activo;
        this.categoria = categoria;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Integer getNumeroCamiseta() {
        return numeroCamiseta;
    }

    public void setNumeroCamiseta(Integer numeroCamiseta) {
        this.numeroCamiseta = numeroCamiseta;
    }

    public String getPosicion() {
        return posicion;
    }

    public void setPosicion(String posicion) {
        this.posicion = posicion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    /**
     * Calcula la edad a la fecha actual (método de la Figura 8). Delega en
     * {@link #getEdad(LocalDate)}.
     *
     * @return edad en años cumplidos
     */
    public int getEdad() {
        return getEdad(LocalDate.now(Fechas.ZONA));
    }

    /**
     * Calcula la edad a una fecha de referencia. Lo usan los servicios con su
     * {@code Clock}, para obtener resultados reproducibles.
     *
     * @param referencia fecha a la que se calcula la edad
     * @return edad en años cumplidos
     */
    public int getEdad(LocalDate referencia) {
        if (fechaNacimiento == null) {
            throw new IllegalStateException("El jugador no tiene fecha de nacimiento");
        }
        return Period.between(fechaNacimiento, referencia).getYears();
    }

    /** {@inheritDoc} Por ejemplo, "DNI 42587634". */
    @Override
    public String getIdentificacion() {
        return "DNI " + dni;
    }

    /**
     * Valida DNI (7 u 8 dígitos), fecha de nacimiento, camiseta (1 a 99,
     * opcional), posición (obligatoria, hasta 30) y categoría. La regla de
     * fecha no futura depende del día actual y la controla el servicio.
     */
    @Override
    protected void validarDatosPropios(List<String> errores) {
        if (Texto.estaVacio(dni)) {
            errores.add("El campo DNI es obligatorio.");
        } else if (!FORMATO_DNI.matcher(dni).matches()) {
            errores.add("El DNI debe tener 7 u 8 dígitos, sin puntos ni espacios.");
        }
        Validaciones.obligatorio(errores, fechaNacimiento, "fecha de nacimiento");
        if (numeroCamiseta != null
                && (numeroCamiseta < CAMISETA_MINIMA || numeroCamiseta > CAMISETA_MAXIMA)) {
            errores.add("El número de camiseta debe estar entre " + CAMISETA_MINIMA
                    + " y " + CAMISETA_MAXIMA + ".");
        }
        Validaciones.texto(errores, posicion, "posición", true, MAX_POSICION);
        Validaciones.obligatorio(errores, categoria, "categoría");
    }
}
