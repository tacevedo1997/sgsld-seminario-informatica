package sgsld.vista;

import java.util.List;

import sgsld.controlador.Controladores;
import sgsld.vista.consola.Consola;

/** Menú del Kinesiólogo: consulta de jugadores y registro clínico. */
public class MenuKinesiologo extends Menu {

    private final Controladores controladores;

    /**
     * Crea el menú.
     *
     * @param consola       entrada y salida
     * @param controladores controladores de los casos de uso
     */
    public MenuKinesiologo(Consola consola, Controladores controladores) {
        super(consola);
        this.controladores = controladores;
    }

    @Override
    protected String getTitulo() {
        return "Kinesiólogo";
    }

    @Override
    protected List<String> getOpciones() {
        return List.of("Consultar jugadores");
    }

    @Override
    protected String getTextoSalida() {
        return "Cerrar sesión";
    }

    @Override
    protected void ejecutarOpcion(int opcion) {
        switch (opcion) {
            case 1 -> new MenuJugadores(getConsola(), controladores.jugadores()).mostrar();
            default -> throw new IllegalArgumentException("Opción inexistente: " + opcion);
        }
    }
}
