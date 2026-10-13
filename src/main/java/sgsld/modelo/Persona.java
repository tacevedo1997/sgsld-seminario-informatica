package sgsld.modelo;

import java.util.ArrayList;
import java.util.List;

import sgsld.util.Validaciones;

/**
 * Abstracción de las personas del dominio (jugadores y kinesiólogos), que
 * comparten nombre, apellido y una identificación propia.
 * <p>
 * {@link #validar()} es un <b>método plantilla</b>: controla los datos comunes
 * y delega en {@link #validarDatosPropios(List)} los de cada subclase.
 */
public abstract class Persona extends Entidad {

    /** Longitud máxima de nombre y apellido ({@code VARCHAR(50)}). */
    public static final int MAX_NOMBRE = 50;

    private String nombre;
    private String apellido;

    /**
     * Crea una persona nueva.
     *
     * @param nombre   nombre de pila
     * @param apellido apellido
     */
    protected Persona(String nombre, String apellido) {
        this(0, nombre, apellido);
    }

    /**
     * Crea una persona con identificador conocido (carga de datos).
     *
     * @param id       identificador
     * @param nombre   nombre de pila
     * @param apellido apellido
     */
    protected Persona(int id, String nombre, String apellido) {
        super(id);
        this.nombre = nombre;
        this.apellido = apellido;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    /**
     * Devuelve el nombre para listados, con el formato de la vista
     * {@code v_estado_jugador}.
     *
     * @return "Apellido, Nombre"
     */
    public String getNombreCompleto() {
        return apellido + ", " + nombre;
    }

    /**
     * Devuelve el dato que identifica a la persona ante el usuario.
     *
     * @return por ejemplo, "DNI 42587634" o "MP-10234"
     */
    public abstract String getIdentificacion();

    /**
     * Valida la persona: primero nombre y apellido (obligatorios, hasta 50
     * caracteres) y luego los datos propios de la subclase.
     *
     * @return lista de errores; vacía si los datos son válidos
     */
    public final List<String> validar() {
        List<String> errores = new ArrayList<>();
        Validaciones.texto(errores, nombre, "nombre", true, MAX_NOMBRE);
        Validaciones.texto(errores, apellido, "apellido", true, MAX_NOMBRE);
        validarDatosPropios(errores);
        return errores;
    }

    /**
     * Paso variable del método plantilla {@link #validar()}: agrega los errores
     * de los datos propios de la subclase.
     *
     * @param errores lista donde se agregan los errores
     */
    protected abstract void validarDatosPropios(List<String> errores);

    @Override
    public String toString() {
        return getNombreCompleto();
    }
}
