package sgsld.modelo;

/**
 * Cuenta de acceso al sistema. La contraseña nunca se guarda en claro: solo
 * su hash PBKDF2 (RNF08).
 */
public class Usuario extends Entidad {

    /** Longitud máxima del nombre de usuario ({@code VARCHAR(30)}). */
    public static final int MAX_NOMBRE_USUARIO = 30;

    private String nombreUsuario;
    private String hashContrasena;
    private Rol rol;
    private boolean activo;

    /**
     * Crea un usuario nuevo y activo.
     *
     * @param nombreUsuario  nombre de inicio de sesión
     * @param hashContrasena hash de la contraseña
     * @param rol            rol asignado
     */
    public Usuario(String nombreUsuario, String hashContrasena, Rol rol) {
        this(0, nombreUsuario, hashContrasena, rol, true);
    }

    /**
     * Crea un usuario con identificador conocido (carga de datos).
     *
     * @param id             identificador
     * @param nombreUsuario  nombre de inicio de sesión
     * @param hashContrasena hash de la contraseña
     * @param rol            rol asignado
     * @param activo         si puede iniciar sesión
     */
    public Usuario(int id, String nombreUsuario, String hashContrasena, Rol rol, boolean activo) {
        super(id);
        this.nombreUsuario = nombreUsuario;
        this.hashContrasena = hashContrasena;
        this.rol = rol;
        this.activo = activo;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    public void setHashContrasena(String hashContrasena) {
        this.hashContrasena = hashContrasena;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /**
     * Indica si el usuario tiene el rol indicado.
     *
     * @param rolBuscado rol a comprobar
     * @return {@code true} si coincide con el rol del usuario
     */
    public boolean tieneRol(Rol rolBuscado) {
        return rol == rolBuscado;
    }

    @Override
    public String toString() {
        return nombreUsuario;
    }
}
