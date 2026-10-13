package sgsld.vista;

import java.util.List;

import sgsld.controlador.KinesiologoController;
import sgsld.util.SgsldException;
import sgsld.vista.consola.Consola;

/** Submenú de kinesiólogos (Coordinador). */
public class MenuKinesiologos extends Menu {

    private final KinesiologoController kinesiologos;

    /**
     * Crea el submenú.
     *
     * @param consola      entrada y salida
     * @param kinesiologos controlador de kinesiólogos
     */
    public MenuKinesiologos(Consola consola, KinesiologoController kinesiologos) {
        super(consola);
        this.kinesiologos = kinesiologos;
    }

    @Override
    protected String getTitulo() {
        return "Kinesiólogos";
    }

    @Override
    protected List<String> getOpciones() {
        return List.of("Listar", "Alta", "Modificar");
    }

    @Override
    protected void ejecutarOpcion(int opcion) throws SgsldException {
        switch (opcion) {
            case 1 -> kinesiologos.listar();
            case 2 -> kinesiologos.alta();
            case 3 -> kinesiologos.modificar();
            default -> throw new IllegalArgumentException("Opción inexistente: " + opcion);
        }
    }
}
