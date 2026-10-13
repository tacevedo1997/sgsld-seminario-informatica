package sgsld.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.Rol;
import sgsld.modelo.Usuario;
import sgsld.util.CredencialesInvalidasException;
import sgsld.util.HashContrasena;
import sgsld.util.SesionUsuario;

/** CPI01 · Inicio de sesión con los usuarios del lote (CU-09). */
@DisplayName("CPI01 · Autenticación")
class AutenticacionTest {

    private final EscenarioPrueba escenario = new EscenarioPrueba();

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    @Test
    @DisplayName("CPI01-1 · lfernandez con la contraseña correcta inicia sesión como Kinesiólogo")
    void credencialesCorrectas() throws Exception {
        Usuario usuario = escenario.autenticacion()
                .iniciarSesion("lfernandez", EscenarioPrueba.CONTRASENA_LOTE.toCharArray());
        assertEquals(Rol.KINESIOLOGO, usuario.getRol());
        assertSame(usuario, SesionUsuario.getInstancia().getUsuario());
        assertTrue(SesionUsuario.getInstancia().puede(Rol.KINESIOLOGO));
        assertFalse(SesionUsuario.getInstancia().puede(Rol.COORDINADOR));
    }

    @Test
    @DisplayName("CPI01-2 · lfernandez con una contraseña incorrecta: CredencialesInvalidasException")
    void contrasenaIncorrecta() {
        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> escenario.autenticacion().iniciarSesion("lfernandez", "Incorrecta1".toCharArray()));
        assertEquals("Usuario o contraseña incorrectos", error.getMessage());
        assertFalse(SesionUsuario.getInstancia().haySesion());
    }

    @Test
    @DisplayName("CPI01-3 · Usuario inexistente: mismo mensaje genérico")
    void usuarioInexistente() {
        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> escenario.autenticacion().iniciarSesion("noexiste",
                        EscenarioPrueba.CONTRASENA_LOTE.toCharArray()));
        assertEquals("Usuario o contraseña incorrectos", error.getMessage());
    }

    @Test
    @DisplayName("CPI01-4 · Los cuatro hashes del lote verifican con Sgsld2026!")
    void hashesDelLote() {
        assertEquals(4, escenario.fabrica().getUsuarioDAO().listarTodos().size());
        for (Usuario usuario : escenario.fabrica().getUsuarioDAO().listarTodos()) {
            assertTrue(HashContrasena.verificar(EscenarioPrueba.CONTRASENA_LOTE, usuario.getHashContrasena()),
                    "Verifica el hash de " + usuario.getNombreUsuario());
        }
    }

    @Test
    @DisplayName("CPI01-5 · Un usuario inactivo no puede iniciar sesión")
    void usuarioInactivo() {
        escenario.fabrica().getUsuarioDAO().buscarPorNombreUsuario("asosa").orElseThrow().setActivo(false);
        assertThrows(CredencialesInvalidasException.class,
                () -> escenario.autenticacion().iniciarSesion("asosa", EscenarioPrueba.CONTRASENA_LOTE.toCharArray()));
    }
}
