package sgsld.vista.fx;

import java.util.ArrayList;
import java.util.List;

import sgsld.modelo.Rol;
import sgsld.util.SesionUsuario;

/**
 * Opción del menú lateral: texto, pantalla FXML y roles que la usan.
 * <p>
 * Las opciones son un <b>arreglo constante</b> ({@link #OPCIONES}): su
 * cantidad es fija y se conoce al compilar. Al iniciar sesión se convierten en
 * un {@code ArrayList} filtrado según los permisos del rol
 * ({@link #paraSesion(SesionUsuario)}).
 *
 * @param texto        texto del menú
 * @param archivoFxml  pantalla que abre
 * @param rolSinAcceso rol que la ve deshabilitada, "(sin acceso)", o {@code null}
 * @param roles        roles que pueden usarla
 */
public record OpcionMenu(String texto, String archivoFxml, Rol rolSinAcceso, Rol... roles) {

    /** Todas las opciones, en el orden del menú (sección 6.3 y Figuras 11 a 13). */
    static final OpcionMenu[] OPCIONES = {
        new OpcionMenu("Jugadores", "jugadores.fxml", null, Rol.COORDINADOR),
        new OpcionMenu("Categorías", "categorias.fxml", null, Rol.COORDINADOR),
        new OpcionMenu("Kinesiólogos", "kinesiologos.fxml", null, Rol.COORDINADOR),
        new OpcionMenu("Consultar ficha", "ficha_jugador.fxml", null, Rol.COORDINADOR, Rol.KINESIOLOGO),
        new OpcionMenu("Registrar lesión", "registro_lesion.fxml", null, Rol.KINESIOLOGO),
        new OpcionMenu("Tratamientos", "tratamientos.fxml", null, Rol.KINESIOLOGO),
        new OpcionMenu("Evoluciones", "evoluciones.fxml", null, Rol.KINESIOLOGO),
        new OpcionMenu("Reportes", "reportes.fxml", Rol.KINESIOLOGO, Rol.COORDINADOR),
        new OpcionMenu("Estado actual", "estado_actual.fxml", null, Rol.CUERPO_TECNICO),
    };

    /**
     * Opciones que corresponden a la sesión: las habilitadas para su rol y las
     * que el rol ve sin acceso.
     *
     * @param sesion sesión iniciada
     * @return las opciones, en el orden del menú
     */
    static List<OpcionMenu> paraSesion(SesionUsuario sesion) {
        Rol rol = sesion.getUsuario().getRol();
        List<OpcionMenu> visibles = new ArrayList<>();
        for (OpcionMenu opcion : OPCIONES) {
            if (opcion.habilitadaPara(sesion) || opcion.rolSinAcceso() == rol) {
                visibles.add(opcion);
            }
        }
        return visibles;
    }

    /**
     * Indica si el rol de la sesión puede usar la opción.
     *
     * @param sesion sesión iniciada
     * @return {@code true} si está habilitada
     */
    boolean habilitadaPara(SesionUsuario sesion) {
        return sesion.puede(roles);
    }

    /**
     * Texto que muestra el menú.
     *
     * @param sesion sesión iniciada
     * @return el texto, con "(sin acceso)" si está deshabilitada
     */
    String textoPara(SesionUsuario sesion) {
        return habilitadaPara(sesion) ? texto : texto + " (sin acceso)";
    }
}
