package sgsld.controlador;

import java.util.Arrays;
import java.util.Optional;

import sgsld.modelo.Usuario;
import sgsld.servicio.AutenticacionService;
import sgsld.util.CredencialesInvalidasException;
import sgsld.util.PersistenciaException;
import sgsld.vista.consola.Consola;

/** Inicio y cierre de sesión (CU-09). */
public class LoginController {

    private final Consola consola;
    private final AutenticacionService autenticacion;

    /**
     * Crea el controlador.
     *
     * @param consola       entrada y salida
     * @param autenticacion servicio de autenticación
     */
    public LoginController(Consola consola, AutenticacionService autenticacion) {
        this.consola = consola;
        this.autenticacion = autenticacion;
    }

    /**
     * Pide usuario y contraseña hasta autenticar o hasta que el usuario
     * desista. Ante credenciales inválidas (CU-09, 2a) o un problema con la
     * base de datos muestra el error y permite reintentar. La contraseña se
     * limpia de la memoria después de usarla.
     *
     * @return el usuario autenticado, o vacío si se canceló
     */
    public Optional<Usuario> iniciarSesion() {
        consola.titulo("Inicio de sesión");
        while (true) {
            String nombreUsuario = consola.leerTexto("Usuario: ", true, Usuario.MAX_NOMBRE_USUARIO);
            char[] contrasena = consola.leerContrasena("Contraseña: ");
            try {
                Usuario usuario = autenticacion.iniciarSesion(nombreUsuario, contrasena);
                consola.mostrarOk("Sesión iniciada: " + usuario.getNombreUsuario() + " (" + usuario.getRol() + ").");
                return Optional.of(usuario);
            } catch (CredencialesInvalidasException | PersistenciaException e) {
                consola.mostrarError(e.getMessage());
            } finally {
                Arrays.fill(contrasena, '\0');
            }
            if (!consola.leerSiNo("¿Desea reintentar? (S/N): ")) {
                return Optional.empty();
            }
        }
    }

    /** Cierra la sesión actual. */
    public void cerrarSesion() {
        autenticacion.cerrarSesion();
        consola.mostrarOk("Sesión cerrada.");
    }
}
