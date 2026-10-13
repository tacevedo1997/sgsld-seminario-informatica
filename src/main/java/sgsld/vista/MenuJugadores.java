package sgsld.vista;

import java.util.ArrayList;
import java.util.List;

import sgsld.controlador.JugadorController;
import sgsld.modelo.Rol;
import sgsld.util.SesionUsuario;
import sgsld.util.SgsldException;
import sgsld.vista.consola.Consola;

/**
 * Submenú de jugadores. Usa {@link SesionUsuario#puede} para decidir qué
 * mostrar: el Coordinador ve todas las opciones; el Kinesiólogo, solo las de
 * consulta.
 */
public class MenuJugadores extends Menu {

    private final JugadorController jugadores;

    /**
     * Crea el submenú.
     *
     * @param consola   entrada y salida
     * @param jugadores controlador de jugadores
     */
    public MenuJugadores(Consola consola, JugadorController jugadores) {
        super(consola);
        this.jugadores = jugadores;
    }

    @Override
    protected String getTitulo() {
        return "Jugadores";
    }

    @Override
    protected List<String> getOpciones() {
        List<String> opciones = new ArrayList<>(List.of("Listar", "Buscar (DNI o apellido)"));
        if (SesionUsuario.getInstancia().puede(Rol.COORDINADOR)) {
            opciones.addAll(List.of("Alta", "Modificar", "Baja lógica"));
        }
        return opciones;
    }

    @Override
    protected void ejecutarOpcion(int opcion) throws SgsldException {
        switch (opcion) {
            case 1 -> jugadores.listar();
            case 2 -> jugadores.buscar();
            case 3 -> jugadores.alta();
            case 4 -> jugadores.modificar();
            case 5 -> jugadores.darDeBaja();
            default -> throw new IllegalArgumentException("Opción inexistente: " + opcion);
        }
    }
}
