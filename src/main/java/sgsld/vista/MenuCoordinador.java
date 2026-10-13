package sgsld.vista;

import java.util.List;

import sgsld.controlador.Controladores;
import sgsld.vista.consola.Consola;

/** Menú del Coordinador: administración de jugadores, categorías y kinesiólogos. */
public class MenuCoordinador extends Menu {

    private final Controladores controladores;

    /**
     * Crea el menú.
     *
     * @param consola       entrada y salida
     * @param controladores controladores de los casos de uso
     */
    public MenuCoordinador(Consola consola, Controladores controladores) {
        super(consola);
        this.controladores = controladores;
    }

    @Override
    protected String getTitulo() {
        return "Coordinador";
    }

    @Override
    protected List<String> getOpciones() {
        return List.of("Gestionar jugadores", "Gestionar categorías", "Gestionar kinesiólogos");
    }

    @Override
    protected String getTextoSalida() {
        return "Cerrar sesión";
    }

    @Override
    protected void ejecutarOpcion(int opcion) {
        switch (opcion) {
            case 1 -> new MenuJugadores(getConsola(), controladores.jugadores()).mostrar();
            case 2 -> new MenuCategorias(getConsola(), controladores.categorias()).mostrar();
            case 3 -> new MenuKinesiologos(getConsola(), controladores.kinesiologos()).mostrar();
            default -> throw new IllegalArgumentException("Opción inexistente: " + opcion);
        }
    }
}
