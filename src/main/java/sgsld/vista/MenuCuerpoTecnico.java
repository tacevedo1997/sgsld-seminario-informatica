package sgsld.vista;

import java.util.List;

import sgsld.controlador.Controladores;
import sgsld.vista.consola.Consola;

/** Menú del Cuerpo Técnico: solo consulta la disponibilidad, sin información clínica. */
public class MenuCuerpoTecnico extends Menu {

    private final Controladores controladores;

    /**
     * Crea el menú.
     *
     * @param consola       entrada y salida
     * @param controladores controladores de los casos de uso
     */
    public MenuCuerpoTecnico(Consola consola, Controladores controladores) {
        super(consola);
        this.controladores = controladores;
    }

    @Override
    protected String getTitulo() {
        return "Cuerpo Técnico";
    }

    @Override
    protected List<String> getOpciones() {
        return List.of();
    }

    @Override
    protected String getTextoSalida() {
        return "Cerrar sesión";
    }

    @Override
    protected void ejecutarOpcion(int opcion) {
        throw new IllegalArgumentException("Opción inexistente: " + opcion);
    }
}
