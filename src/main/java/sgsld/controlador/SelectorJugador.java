package sgsld.controlador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import sgsld.modelo.Jugador;
import sgsld.modelo.Persona;
import sgsld.servicio.JugadorService;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.PersistenciaException;
import sgsld.vista.consola.Consola;

/**
 * Selección de un jugador por DNI (búsqueda binaria), por apellido (búsqueda
 * secuencial) o de la lista completa. La comparten los flujos que necesitan
 * elegir un jugador: administración, registro de lesiones y ficha.
 */
public class SelectorJugador {

    private final Consola consola;
    private final JugadorService jugadores;

    /**
     * Crea el selector.
     *
     * @param consola   entrada y salida
     * @param jugadores servicio de jugadores
     */
    public SelectorJugador(Consola consola, JugadorService jugadores) {
        this.consola = consola;
        this.jugadores = jugadores;
    }

    /**
     * Describe un jugador en una línea, para listas de selección.
     *
     * @param jugador jugador a describir
     * @return por ejemplo, "Gómez, Pedro - DNI 42587634 - Primera"
     */
    public static String describir(Jugador jugador) {
        return jugador.getNombreCompleto() + " - " + jugador.getIdentificacion() + " - "
                + jugador.getCategoria().getNombre() + (jugador.isActivo() ? "" : " (baja)");
    }

    /**
     * Permite elegir un jugador.
     *
     * @param soloActivos {@code true} para descartar a los dados de baja
     * @return el jugador elegido, o vacío si no hubo resultados o se canceló
     * @throws AccesoDenegadoException si el rol no puede consultar jugadores
     */
    public Optional<Jugador> seleccionar(boolean soloActivos)
            throws AccesoDenegadoException, PersistenciaException {
        consola.mostrar("Buscar jugador por:");
        consola.mostrar("  1. DNI");
        consola.mostrar("  2. Apellido");
        consola.mostrar("  3. Lista de jugadores");
        consola.mostrar("  0. Cancelar");
        int forma = consola.leerEntero("Opción: ", 0, 3);
        return switch (forma) {
            case 1 -> porDni(soloActivos);
            case 2 -> porApellido(soloActivos);
            case 3 -> deLaLista(soloActivos);
            default -> Optional.empty();
        };
    }

    private Optional<Jugador> porDni(boolean soloActivos) throws AccesoDenegadoException, PersistenciaException {
        String dni = consola.leerTexto("DNI (sin puntos): ", true, 10);
        try {
            Jugador jugador = jugadores.buscarPorDni(dni);
            if (soloActivos && !jugador.isActivo()) {
                consola.mostrarError("El jugador " + jugador.getNombreCompleto() + " está dado de baja.");
                return Optional.empty();
            }
            consola.mostrar("Jugador: " + describir(jugador));
            return Optional.of(jugador);
        } catch (EntidadNoEncontradaException e) {
            consola.mostrarAviso("No se encontraron resultados. " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Jugador> porApellido(boolean soloActivos)
            throws AccesoDenegadoException, PersistenciaException {
        String apellido = consola.leerTexto("Apellido (o parte): ", true, Persona.MAX_NOMBRE);
        List<Jugador> encontrados = new ArrayList<>();
        for (Jugador jugador : jugadores.buscarPorApellido(apellido)) {
            if (!soloActivos || jugador.isActivo()) {
                encontrados.add(jugador);
            }
        }
        if (encontrados.isEmpty()) {
            consola.mostrarAviso("No se encontraron resultados.");
            return Optional.empty();
        }
        if (encontrados.size() == 1) {
            consola.mostrar("Jugador: " + describir(encontrados.get(0)));
            return Optional.of(encontrados.get(0));
        }
        return consola.elegirDeLista("Coincidencias:", encontrados, SelectorJugador::describir);
    }

    private Optional<Jugador> deLaLista(boolean soloActivos)
            throws AccesoDenegadoException, PersistenciaException {
        return consola.elegirDeLista("Jugadores:", jugadores.listar(!soloActivos), SelectorJugador::describir);
    }
}
