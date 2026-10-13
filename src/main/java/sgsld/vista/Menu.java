package sgsld.vista;

import java.util.List;

import sgsld.modelo.Usuario;
import sgsld.util.SesionUsuario;
import sgsld.util.SgsldException;
import sgsld.vista.consola.Consola;

/**
 * Menú de selección de la consola. Raíz de la jerarquía de menús.
 * <p>
 * {@link #mostrar()} es un <b>método plantilla</b>: repite el ciclo de
 * encabezado, opciones y lectura de la opción (validada contra el rango), sale
 * con 0 y, si no, delega en {@link #ejecutarOpcion(int)}, que cada subclase
 * implementa con un {@code switch}. Las subclases definen el título, las
 * opciones y la acción de cada opción.
 */
public abstract class Menu {

    private final Consola consola;

    /**
     * Crea el menú.
     *
     * @param consola entrada y salida
     */
    protected Menu(Consola consola) {
        this.consola = consola;
    }

    /**
     * Muestra el menú hasta que se elija 0. Captura {@link SgsldException}
     * como red de seguridad: muestra el mensaje y vuelve a mostrar el menú.
     */
    public final void mostrar() {
        consola.entrarA(getTitulo());
        try {
            int opcion;
            do {
                imprimirEncabezado();
                List<String> opciones = getOpciones();
                for (int i = 0; i < opciones.size(); i++) {
                    consola.mostrar("  " + (i + 1) + ". " + opciones.get(i));
                }
                consola.mostrar("  0. " + getTextoSalida());
                opcion = consola.leerEntero("Opción: ", 0, opciones.size());
                if (opcion != 0) {
                    try {
                        ejecutarOpcion(opcion);
                    } catch (SgsldException e) {
                        consola.mostrarError(e.getMessage());
                    }
                }
            } while (opcion != 0);
        } finally {
            consola.salirDeNivel();
        }
    }

    /**
     * Título del menú; es el nivel que se agrega a la ruta de navegación.
     *
     * @return el título
     */
    protected abstract String getTitulo();

    /**
     * Opciones del menú, numeradas desde 1 al mostrarlas.
     *
     * @return los textos de las opciones
     */
    protected abstract List<String> getOpciones();

    /**
     * Ejecuta la opción elegida.
     *
     * @param opcion número de opción, entre 1 y la cantidad de opciones
     * @throws SgsldException si la operación falla por una regla del dominio
     */
    protected abstract void ejecutarOpcion(int opcion) throws SgsldException;

    /**
     * Texto de la opción 0. Por defecto, "Volver".
     *
     * @return el texto de la opción de salida
     */
    protected String getTextoSalida() {
        return "Volver";
    }

    /**
     * Da acceso a la consola a las subclases.
     *
     * @return la consola
     */
    protected Consola getConsola() {
        return consola;
    }

    private void imprimirEncabezado() {
        Usuario usuario = SesionUsuario.getInstancia().getUsuario();
        consola.lineaEnBlanco();
        consola.separador('=');
        consola.mostrar(consola.getRuta());
        consola.mostrar(usuario == null
                ? "Sin sesión iniciada"
                : "Usuario: " + usuario.getNombreUsuario() + " | Rol: " + usuario.getRol());
        consola.separador('-');
    }
}
