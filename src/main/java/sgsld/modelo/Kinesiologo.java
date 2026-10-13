package sgsld.modelo;

import java.util.List;

import sgsld.util.Validaciones;

/**
 * Kinesiólogo del club. Puede tener un usuario para operar el sistema
 * (relación 0..1): un kinesiólogo sin usuario puede figurar como responsable
 * de tratamientos, pero no iniciar sesión.
 */
public class Kinesiologo extends Persona {

    /** Longitud máxima de la matrícula ({@code VARCHAR(20)}). */
    public static final int MAX_MATRICULA = 20;

    private String matricula;
    private Usuario usuario;

    /**
     * Crea un kinesiólogo nuevo.
     *
     * @param nombre    nombre de pila
     * @param apellido  apellido
     * @param matricula matrícula profesional, única
     * @param usuario   usuario asociado o {@code null}
     */
    public Kinesiologo(String nombre, String apellido, String matricula, Usuario usuario) {
        this(0, nombre, apellido, matricula, usuario);
    }

    /**
     * Crea un kinesiólogo con identificador conocido (carga de datos).
     *
     * @param id        identificador
     * @param nombre    nombre de pila
     * @param apellido  apellido
     * @param matricula matrícula profesional, única
     * @param usuario   usuario asociado o {@code null}
     */
    public Kinesiologo(int id, String nombre, String apellido, String matricula, Usuario usuario) {
        super(id, nombre, apellido);
        this.matricula = matricula;
        this.usuario = usuario;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    /**
     * Devuelve el usuario asociado.
     *
     * @return el usuario, o {@code null} si el kinesiólogo no opera el sistema
     */
    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    /**
     * Indica si el kinesiólogo tiene usuario asociado.
     *
     * @return {@code true} si tiene usuario
     */
    public boolean tieneUsuario() {
        return usuario != null;
    }

    /** {@inheritDoc} Es la matrícula, por ejemplo, "MP-10234". */
    @Override
    public String getIdentificacion() {
        return matricula;
    }

    /** Valida la matrícula: obligatoria, hasta 20 caracteres. */
    @Override
    protected void validarDatosPropios(List<String> errores) {
        Validaciones.texto(errores, matricula, "matrícula", true, MAX_MATRICULA);
    }
}
