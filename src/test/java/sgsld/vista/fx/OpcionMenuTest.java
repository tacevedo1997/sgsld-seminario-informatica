package sgsld.vista.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.Rol;
import sgsld.modelo.Usuario;
import sgsld.util.SesionUsuario;

/** Menú lateral de la interfaz JavaFX según el rol (sección 6.3 y Figuras 11 a 13). */
@DisplayName("Interfaz JavaFX · menú lateral por rol")
class OpcionMenuTest {

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    /** Opciones del rol como "texto" o "texto [deshabilitada]". */
    private static List<String> menuDe(Rol rol) {
        SesionUsuario sesion = SesionUsuario.getInstancia();
        sesion.iniciar(new Usuario(1, "prueba", "-", rol, true));
        List<String> menu = new ArrayList<>();
        for (OpcionMenu opcion : OpcionMenu.paraSesion(sesion)) {
            menu.add(opcion.textoPara(sesion) + (opcion.habilitadaPara(sesion) ? "" : " [deshabilitada]"));
        }
        return menu;
    }

    @Test
    @DisplayName("Coordinador: Jugadores · Categorías · Kinesiólogos · Consultar ficha · Reportes (Figura 13)")
    void coordinador() {
        assertEquals(List.of("Jugadores", "Categorías", "Kinesiólogos", "Consultar ficha", "Reportes"),
                menuDe(Rol.COORDINADOR));
    }

    @Test
    @DisplayName("Kinesiólogo: registro clínico y \"Reportes (sin acceso)\" deshabilitado (Figura 11)")
    void kinesiologo() {
        assertEquals(List.of("Consultar ficha", "Registrar lesión", "Tratamientos", "Evoluciones",
                "Reportes (sin acceso) [deshabilitada]"), menuDe(Rol.KINESIOLOGO));
    }

    @Test
    @DisplayName("Cada opción del menú tiene su pantalla FXML")
    void cadaOpcionTieneSuPantalla() {
        for (OpcionMenu opcion : OpcionMenu.OPCIONES) {
            assertNotNull(OpcionMenu.class.getResource(opcion.archivoFxml()), opcion.archivoFxml());
        }
    }

    @Test
    @DisplayName("Cuerpo Técnico: solo Estado actual (Figura 12)")
    void cuerpoTecnico() {
        assertEquals(List.of("Estado actual"), menuDe(Rol.CUERPO_TECNICO));
    }
}
