package sgsld.controlador;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import sgsld.modelo.Categoria;
import sgsld.modelo.Jugador;
import sgsld.modelo.Persona;
import sgsld.servicio.CategoriaService;
import sgsld.servicio.JugadorService;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.Fechas;
import sgsld.util.PersistenciaException;
import sgsld.util.ValidacionException;
import sgsld.vista.consola.Consola;
import sgsld.vista.consola.TablaConsola;

/**
 * Gestión de jugadores (CU-01): listar, buscar, alta, modificación y baja
 * lógica. Ante errores de validación muestra todos los errores y permite
 * corregir los datos o cancelar.
 */
public class JugadorController {

    private final Consola consola;
    private final JugadorService jugadores;
    private final CategoriaService categorias;
    private final SelectorJugador selector;

    /**
     * Crea el controlador.
     *
     * @param consola    entrada y salida
     * @param jugadores  servicio de jugadores
     * @param categorias servicio de categorías
     * @param selector   selección de jugadores
     */
    public JugadorController(Consola consola, JugadorService jugadores, CategoriaService categorias,
                             SelectorJugador selector) {
        this.consola = consola;
        this.jugadores = jugadores;
        this.categorias = categorias;
        this.selector = selector;
    }

    /**
     * Lista los jugadores en una tabla, con la opción de incluir a los dados de baja.
     *
     * @throws AccesoDenegadoException si el rol no puede consultar jugadores
     */
    public void listar() throws AccesoDenegadoException, PersistenciaException {
        boolean incluirInactivos = consola.leerSiNo("¿Incluir jugadores dados de baja? (S/N): ");
        List<Jugador> lista = jugadores.listar(incluirInactivos);
        consola.titulo(incluirInactivos ? "Jugadores (activos y dados de baja)" : "Jugadores activos");
        mostrarTabla(lista, incluirInactivos);
        consola.mostrar("Total: " + lista.size() + (lista.size() == 1 ? " jugador." : " jugadores."));
    }

    /**
     * Busca jugadores por DNI (búsqueda binaria) o por apellido (búsqueda secuencial).
     *
     * @throws AccesoDenegadoException si el rol no puede consultar jugadores
     */
    public void buscar() throws AccesoDenegadoException, PersistenciaException {
        consola.mostrar("Buscar por:");
        consola.mostrar("  1. DNI (búsqueda binaria en el índice por DNI)");
        consola.mostrar("  2. Apellido (búsqueda secuencial, coincidencia parcial)");
        consola.mostrar("  0. Cancelar");
        int forma = consola.leerEntero("Opción: ", 0, 2);
        if (forma == 1) {
            String dni = consola.leerTexto("DNI (sin puntos): ", true, 10);
            try {
                mostrarTabla(List.of(jugadores.buscarPorDni(dni)), true);
            } catch (EntidadNoEncontradaException e) {
                consola.mostrarAviso("No se encontraron resultados. " + e.getMessage());
            }
        } else if (forma == 2) {
            String apellido = consola.leerTexto("Apellido (o parte): ", true, Persona.MAX_NOMBRE);
            List<Jugador> encontrados = jugadores.buscarPorApellido(apellido);
            if (encontrados.isEmpty()) {
                consola.mostrarAviso("No se encontraron resultados.");
            } else {
                mostrarTabla(encontrados, true);
            }
        }
    }

    /**
     * Da de alta un jugador. La categoría se elige de una lista; si la edad no
     * corresponde a su rango, se avisa sin impedir el registro.
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public void alta() throws AccesoDenegadoException, PersistenciaException {
        List<Categoria> disponibles = categorias.listar();
        if (disponibles.isEmpty()) {
            consola.mostrarError("Debe registrar al menos una categoría antes de dar de alta jugadores.");
            return;
        }
        consola.titulo("Alta de jugador");
        Jugador base = null;
        while (true) {
            Optional<Jugador> candidato = pedirDatos(base, disponibles);
            if (candidato.isEmpty()) {
                consola.mostrar("Operación cancelada.");
                return;
            }
            avisarEdad(candidato.get());
            try {
                int id = jugadores.registrar(candidato.get());
                consola.mostrarOk("Jugador registrado con el N° " + id + ": " + candidato.get().getNombreCompleto() + ".");
                return;
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                if (!consola.leerSiNo("¿Desea corregir los datos? (S/N): ")) {
                    consola.mostrar("Operación cancelada.");
                    return;
                }
                base = candidato.get();
            }
        }
    }

    /**
     * Modifica los datos de un jugador; Enter conserva cada valor actual.
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public void modificar() throws AccesoDenegadoException, PersistenciaException {
        Optional<Jugador> elegido = selector.seleccionar(false);
        if (elegido.isEmpty()) {
            return;
        }
        List<Categoria> disponibles = categorias.listar();
        consola.titulo("Modificar jugador: " + elegido.get().getNombreCompleto());
        consola.mostrar("Presione Enter para conservar el valor actual de cada dato.");
        Jugador base = elegido.get();
        while (true) {
            Jugador candidato = pedirDatos(base, disponibles).orElse(base);
            avisarEdad(candidato);
            try {
                jugadores.modificar(candidato);
                consola.mostrarOk("Datos de " + candidato.getNombreCompleto() + " actualizados.");
                return;
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                if (!consola.leerSiNo("¿Desea corregir los datos? (S/N): ")) {
                    consola.mostrar("Operación cancelada: no se modificó el jugador.");
                    return;
                }
                base = candidato;
            } catch (EntidadNoEncontradaException e) {
                consola.mostrarError(e.getMessage());
                return;
            }
        }
    }

    /**
     * Baja lógica de un jugador, con confirmación (CU-01, 3b).
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public void darDeBaja() throws AccesoDenegadoException, PersistenciaException {
        Optional<Jugador> elegido = selector.seleccionar(true);
        if (elegido.isEmpty()) {
            return;
        }
        Jugador jugador = elegido.get();
        if (!consola.leerSiNo("¿Confirma la baja lógica de " + jugador.getNombreCompleto() + " ("
                + jugador.getIdentificacion() + ")? (S/N): ")) {
            consola.mostrar("Operación cancelada: el jugador sigue activo.");
            return;
        }
        try {
            jugadores.darDeBaja(jugador.getId());
            consola.mostrarOk("Jugador dado de baja. Su historial clínico se conserva.");
        } catch (EntidadNoEncontradaException | ValidacionException e) {
            consola.mostrarError(e.getMessage());
        }
    }

    /**
     * Pide los datos del formulario. En una modificación o corrección, Enter
     * conserva el valor de {@code base}.
     *
     * @return el jugador candidato, o vacío si se canceló la elección de categoría en un alta
     */
    private Optional<Jugador> pedirDatos(Jugador base, List<Categoria> disponibles) {
        boolean hayBase = base != null;
        String dni = consola.editarTexto("DNI (7 u 8 dígitos, sin puntos)", hayBase ? base.getDni() : null, true, 10);
        String nombre = consola.editarTexto("Nombre", hayBase ? base.getNombre() : null, true, Persona.MAX_NOMBRE);
        String apellido = consola.editarTexto("Apellido", hayBase ? base.getApellido() : null, true,
                Persona.MAX_NOMBRE);
        LocalDate nacimiento = hayBase && base.getFechaNacimiento() != null
                ? consola.leerFecha("Fecha de nacimiento ", base.getFechaNacimiento())
                : consola.leerFecha("Fecha de nacimiento (dd/mm/aaaa): ");
        Integer camiseta = leerCamiseta(hayBase ? base.getNumeroCamiseta() : null, hayBase);
        String posicion = consola.editarTexto("Posición", hayBase ? base.getPosicion() : null, true,
                Jugador.MAX_POSICION);
        if (hayBase) {
            consola.mostrar("Categoría actual: " + base.getCategoria() + ". Elija otra o 0 para conservarla.");
        }
        Optional<Categoria> elegida = consola.elegirDeLista("Categoría:", disponibles,
                categoria -> categoria.getNombre() + " (" + categoria.getRangoEtario() + ")");
        if (elegida.isEmpty() && !hayBase) {
            return Optional.empty();
        }
        Categoria categoria = elegida.isPresent() ? elegida.get() : base.getCategoria();
        return Optional.of(new Jugador(hayBase ? base.getId() : 0, dni, nombre, apellido, nacimiento, camiseta,
                posicion, !hayBase || base.isActivo(), categoria));
    }

    private Integer leerCamiseta(Integer actual, boolean hayBase) {
        if (!hayBase) {
            return consola.leerEnteroOpcional("Número de camiseta (1 a 99, Enter = sin número): ",
                    Jugador.CAMISETA_MINIMA, Jugador.CAMISETA_MAXIMA);
        }
        Integer valor = consola.leerEnteroOpcional("Número de camiseta [" + (actual == null ? "sin número" : actual)
                + "] (0 = sin número): ", 0, Jugador.CAMISETA_MAXIMA);
        if (valor == null) {
            return actual;
        }
        return valor == 0 ? null : valor;
    }

    /** Aviso no bloqueante si la edad no corresponde al rango de la categoría. */
    private void avisarEdad(Jugador jugador) {
        LocalDate hoy = jugadores.hoy();
        Categoria categoria = jugador.getCategoria();
        if (categoria == null || jugador.getFechaNacimiento() == null || jugador.getFechaNacimiento().isAfter(hoy)) {
            return;
        }
        int edad = jugador.getEdad(hoy);
        if (!categoria.admiteEdad(edad)) {
            consola.mostrarAviso("La edad del jugador (" + edad + " años) no corresponde al rango de la categoría "
                    + categoria.getNombre() + " (" + categoria.getRangoEtario() + "). Puede continuar con el registro.");
        }
    }

    private void mostrarTabla(List<Jugador> lista, boolean conEstado) {
        TablaConsola tabla = conEstado
                ? new TablaConsola("DNI", "Jugador", "Nacimiento", "Categoría", "Posición", "Camiseta", "Estado")
                : new TablaConsola("DNI", "Jugador", "Nacimiento", "Categoría", "Posición", "Camiseta");
        tabla.alinearDerecha(0, 5);
        for (Jugador jugador : lista) {
            String camiseta = jugador.getNumeroCamiseta() == null ? null : String.valueOf(jugador.getNumeroCamiseta());
            tabla.agregarFila(jugador.getDni(), jugador.getNombreCompleto(), Fechas.formatear(jugador.getFechaNacimiento()),
                    jugador.getCategoria().getNombre(), jugador.getPosicion(), camiseta,
                    jugador.isActivo() ? "Activo" : "Baja");
        }
        tabla.imprimir(consola);
    }
}
