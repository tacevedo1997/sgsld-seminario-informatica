package sgsld.controlador;

import java.util.List;
import java.util.Optional;

import sgsld.modelo.Categoria;
import sgsld.servicio.CategoriaService;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.PersistenciaException;
import sgsld.util.ValidacionException;
import sgsld.vista.consola.Consola;
import sgsld.vista.consola.TablaConsola;

/** Gestión de categorías (CU-02): listar, alta y modificación. */
public class CategoriaController {

    private final Consola consola;
    private final CategoriaService categorias;

    /**
     * Crea el controlador.
     *
     * @param consola    entrada y salida
     * @param categorias servicio de categorías
     */
    public CategoriaController(Consola consola, CategoriaService categorias) {
        this.consola = consola;
        this.categorias = categorias;
    }

    /**
     * Lista las categorías con su rango etario.
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public void listar() throws AccesoDenegadoException, PersistenciaException {
        List<Categoria> lista = categorias.listar();
        consola.titulo("Categorías");
        TablaConsola tabla = new TablaConsola("N°", "Categoría", "Edad mínima", "Edad máxima").alinearDerecha(0, 2, 3);
        for (Categoria categoria : lista) {
            tabla.agregarFila(String.valueOf(categoria.getId()), categoria.getNombre(),
                    String.valueOf(categoria.getEdadMin()), String.valueOf(categoria.getEdadMax()));
        }
        tabla.imprimir(consola);
    }

    /**
     * Registra una categoría.
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public void alta() throws AccesoDenegadoException, PersistenciaException {
        consola.titulo("Alta de categoría");
        Categoria base = null;
        while (true) {
            Categoria candidata = pedirDatos(base);
            try {
                int id = categorias.registrar(candidata);
                consola.mostrarOk("Categoría registrada con el N° " + id + ": " + candidata.getNombre() + ".");
                return;
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                if (!consola.leerSiNo("¿Desea corregir los datos? (S/N): ")) {
                    consola.mostrar("Operación cancelada.");
                    return;
                }
                base = candidata;
            }
        }
    }

    /**
     * Modifica una categoría elegida de la lista; Enter conserva cada valor.
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public void modificar() throws AccesoDenegadoException, PersistenciaException {
        Optional<Categoria> elegida = consola.elegirDeLista("Categoría a modificar:", categorias.listar(),
                categoria -> categoria.getNombre() + " (" + categoria.getRangoEtario() + ")");
        if (elegida.isEmpty()) {
            return;
        }
        consola.mostrar("Presione Enter para conservar el valor actual de cada dato.");
        Categoria base = elegida.get();
        while (true) {
            Categoria candidata = pedirDatos(base);
            try {
                categorias.modificar(candidata);
                consola.mostrarOk("Categoría " + candidata.getNombre() + " actualizada.");
                return;
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                if (!consola.leerSiNo("¿Desea corregir los datos? (S/N): ")) {
                    consola.mostrar("Operación cancelada: no se modificó la categoría.");
                    return;
                }
                base = candidata;
            } catch (EntidadNoEncontradaException e) {
                consola.mostrarError(e.getMessage());
                return;
            }
        }
    }

    private Categoria pedirDatos(Categoria base) {
        boolean hayBase = base != null;
        String nombre = consola.editarTexto("Nombre", hayBase ? base.getNombre() : null, true, Categoria.MAX_NOMBRE);
        int edadMin = consola.editarEntero("Edad mínima (0 a 99)", hayBase ? base.getEdadMin() : null,
                Categoria.EDAD_MINIMA, Categoria.EDAD_MAXIMA);
        int edadMax = consola.editarEntero("Edad máxima (0 a 99)", hayBase ? base.getEdadMax() : null,
                Categoria.EDAD_MINIMA, Categoria.EDAD_MAXIMA);
        return new Categoria(hayBase ? base.getId() : 0, nombre, edadMin, edadMax);
    }
}
