package sgsld.servicio;

import java.util.Optional;

import sgsld.dao.UsuarioDAO;
import sgsld.modelo.Usuario;
import sgsld.util.CredencialesInvalidasException;
import sgsld.util.HashContrasena;
import sgsld.util.SesionUsuario;

/** Inicio y cierre de sesión (CU-09). No requiere sesión previa. */
public class AutenticacionService {

    /**
     * Hash con formato válido que no corresponde a ninguna contraseña conocida.
     * Se verifica cuando el usuario no existe, para que el tiempo de respuesta
     * no revele qué nombres de usuario están registrados.
     */
    private static final String HASH_FICTICIO =
            "pbkdf2_sha256$600000$AAAAAAAAAAAAAAAAAAAAAA==$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private final UsuarioDAO usuarios;

    /**
     * Crea el servicio.
     *
     * @param usuarios DAO de usuarios
     */
    public AutenticacionService(UsuarioDAO usuarios) {
        this.usuarios = usuarios;
    }

    /**
     * Autentica al usuario e inicia su sesión. La contraseña se recibe como
     * {@code char[]} para que el llamador pueda limpiarla después.
     *
     * @param nombreUsuario nombre de usuario
     * @param contrasena    contraseña ingresada
     * @return el usuario autenticado
     * @throws CredencialesInvalidasException si el usuario no existe, está
     *         inactivo o la contraseña no coincide (mensaje genérico)
     */
    public Usuario iniciarSesion(String nombreUsuario, char[] contrasena) throws CredencialesInvalidasException {
        Optional<Usuario> encontrado = nombreUsuario == null
                ? Optional.empty()
                : usuarios.buscarPorNombreUsuario(nombreUsuario.trim());
        String hash = encontrado.isPresent() ? encontrado.get().getHashContrasena() : HASH_FICTICIO;
        boolean contrasenaCorrecta = HashContrasena.verificar(contrasena, hash);
        if (encontrado.isEmpty() || !encontrado.get().isActivo() || !contrasenaCorrecta) {
            throw new CredencialesInvalidasException();
        }
        SesionUsuario.getInstancia().iniciar(encontrado.get());
        return encontrado.get();
    }

    /** Cierra la sesión actual. */
    public void cerrarSesion() {
        SesionUsuario.getInstancia().cerrar();
    }
}
