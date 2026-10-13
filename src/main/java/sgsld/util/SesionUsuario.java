package sgsld.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import sgsld.modelo.Rol;
import sgsld.modelo.Usuario;

/**
 * Sesión del usuario que opera el sistema (patrón <b>Singleton</b>, Figura 9).
 * <p>
 * Hay una única sesión por ejecución: el constructor es privado y la instancia
 * se obtiene con {@link #getInstancia()}. Los menús usan {@link #puede(Rol...)}
 * para decidir qué opciones mostrar, y los servicios usan
 * {@link #exigir(Rol...)} para volver a controlar el rol antes de operar.
 */
public final class SesionUsuario {

    /** Instancia única, creada al cargar la clase (inicialización segura entre hilos). */
    private static final SesionUsuario INSTANCIA = new SesionUsuario();

    private Usuario usuarioActual;

    private SesionUsuario() {
    }

    /**
     * Devuelve la instancia única de la sesión.
     *
     * @return la sesión
     */
    public static SesionUsuario getInstancia() {
        return INSTANCIA;
    }

    /**
     * Inicia la sesión de un usuario ya autenticado.
     *
     * @param usuario usuario autenticado
     */
    public void iniciar(Usuario usuario) {
        this.usuarioActual = Objects.requireNonNull(usuario, "usuario");
    }

    /**
     * Devuelve el usuario de la sesión.
     *
     * @return el usuario, o {@code null} si no hay sesión iniciada
     */
    public Usuario getUsuario() {
        return usuarioActual;
    }

    /**
     * Indica si hay una sesión iniciada.
     *
     * @return {@code true} si hay un usuario en sesión
     */
    public boolean haySesion() {
        return usuarioActual != null;
    }

    /**
     * Indica si el usuario de la sesión tiene alguno de los roles indicados.
     *
     * @param roles roles admitidos
     * @return {@code true} si hay sesión y el rol del usuario está entre los indicados
     */
    public boolean puede(Rol... roles) {
        if (usuarioActual == null) {
            return false;
        }
        for (Rol rol : roles) {
            if (usuarioActual.tieneRol(rol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Exige que el usuario de la sesión tenga alguno de los roles indicados.
     *
     * @param roles roles admitidos
     * @throws AccesoDenegadoException si no hay sesión o el rol no está permitido
     */
    public void exigir(Rol... roles) throws AccesoDenegadoException {
        if (usuarioActual == null) {
            throw new AccesoDenegadoException("Debe iniciar sesión para realizar esta operación.");
        }
        if (!puede(roles)) {
            List<String> permitidos = new ArrayList<>();
            for (Rol rol : roles) {
                permitidos.add(rol.getEtiqueta());
            }
            throw new AccesoDenegadoException("Acceso denegado: el rol " + usuarioActual.getRol()
                    + " no puede realizar esta operación (requiere " + String.join(" o ", permitidos) + ").");
        }
    }

    /** Cierra la sesión actual. */
    public void cerrar() {
        usuarioActual = null;
    }
}
