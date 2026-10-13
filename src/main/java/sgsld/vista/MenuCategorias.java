package sgsld.vista;

import java.util.List;

import sgsld.controlador.CategoriaController;
import sgsld.util.SgsldException;
import sgsld.vista.consola.Consola;

/** Submenú de categorías (Coordinador). */
public class MenuCategorias extends Menu {

    private final CategoriaController categorias;

    /**
     * Crea el submenú.
     *
     * @param consola    entrada y salida
     * @param categorias controlador de categorías
     */
    public MenuCategorias(Consola consola, CategoriaController categorias) {
        super(consola);
        this.categorias = categorias;
    }

    @Override
    protected String getTitulo() {
        return "Categorías";
    }

    @Override
    protected List<String> getOpciones() {
        return List.of("Listar", "Alta", "Modificar");
    }

    @Override
    protected void ejecutarOpcion(int opcion) throws SgsldException {
        switch (opcion) {
            case 1 -> categorias.listar();
            case 2 -> categorias.alta();
            case 3 -> categorias.modificar();
            default -> throw new IllegalArgumentException("Opción inexistente: " + opcion);
        }
    }
}
