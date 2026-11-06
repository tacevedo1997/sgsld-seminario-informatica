package sgsld.vista;

import java.util.List;
import java.util.Optional;

import sgsld.controlador.Controladores;
import sgsld.modelo.Rol;
import sgsld.modelo.Usuario;
import sgsld.vista.consola.Consola;

/**
 * Menú inicial: iniciar sesión o salir. Tras iniciar sesión, un método de
 * fábrica elige el menú según el rol y lo muestra a través de una referencia
 * de tipo {@link Menu} (polimorfismo).
 */
public class MenuPrincipal extends Menu {

    private final Controladores controladores;

    /**
     * Crea el menú principal.
     *
     * @param consola       entrada y salida
     * @param controladores controladores de los casos de uso
     */
    public MenuPrincipal(Consola consola, Controladores controladores) {
        super(consola);
        this.controladores = controladores;
    }

    /**
     * Muestra la pantalla inicial con el nombre del sistema y los avisos (modo
     * de datos, fecha fijada...). Se muestra antes de conectar con la base.
     *
     * @param consola consola de salida
     * @param avisos  avisos de arranque
     */
    public static void mostrarPantallaInicial(Consola consola, List<String> avisos) {
        consola.separador('=');
        consola.mostrar("SGSLD - Sistema de Gestión y Seguimiento de Lesiones Deportivas");
        consola.separador('=');
        for (String aviso : avisos) {
            consola.mostrar(aviso);
        }
    }

    /** Es la base de la ruta de navegación. */
    @Override
    protected String getTitulo() {
        return "SGSLD";
    }

    @Override
    protected List<String> getOpciones() {
        return List.of("Iniciar sesión");
    }

    @Override
    protected String getTextoSalida() {
        return "Salir";
    }

    @Override
    protected void ejecutarOpcion(int opcion) {
        switch (opcion) {
            case 1 -> {
                Optional<Usuario> usuario = controladores.login().iniciarSesion();
                if (usuario.isPresent()) {
                    Menu menuDelRol = crearMenu(usuario.get().getRol());
                    menuDelRol.mostrar();
                    controladores.login().cerrarSesion();
                }
            }
            default -> throw new IllegalArgumentException("Opción inexistente: " + opcion);
        }
    }

    /**
     * Método de fábrica: crea el menú que corresponde al rol. Cada menú ofrece
     * solo lo permitido al rol; los servicios vuelven a verificarlo.
     *
     * @param rol rol del usuario de la sesión
     * @return el menú del rol
     */
    private Menu crearMenu(Rol rol) {
        return switch (rol) {
            case COORDINADOR -> new MenuCoordinador(getConsola(), controladores);
            case KINESIOLOGO -> new MenuKinesiologo(getConsola(), controladores);
            case CUERPO_TECNICO -> new MenuCuerpoTecnico(getConsola(), controladores);
        };
    }
}
