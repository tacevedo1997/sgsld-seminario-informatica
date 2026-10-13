package sgsld.controlador;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.Persona;
import sgsld.modelo.dto.EstadoJugadorDTO;
import sgsld.servicio.ConsultaService;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.Texto;
import sgsld.vista.consola.Consola;
import sgsld.vista.consola.TablaConsola;

/**
 * Estado actual de disponibilidad (CU-08). Vista de solo lectura, sin datos
 * clínicos: es lo único que consulta el Cuerpo Técnico.
 */
public class EstadoActualController {

    private final Consola consola;
    private final ConsultaService consultas;

    /**
     * Crea el controlador.
     *
     * @param consola   entrada y salida
     * @param consultas servicio de consultas
     */
    public EstadoActualController(Consola consola, ConsultaService consultas) {
        this.consola = consola;
        this.consultas = consultas;
    }

    /**
     * Muestra el estado de los jugadores activos, con filtros opcionales por
     * categoría y por apellido.
     *
     * @throws AccesoDenegadoException si no hay sesión iniciada
     */
    public void consultarEstado() throws AccesoDenegadoException {
        consola.titulo("Estado actual de disponibilidad");
        List<EstadoJugadorDTO> filas = consultas.consultarEstadoActual();
        Optional<String> categoria = Optional.empty();
        if (consola.leerSiNo("¿Filtrar por categoría? (S/N): ")) {
            categoria = consola.elegirDeLista("Categoría:", categoriasPresentes(filas), nombre -> nombre);
        }
        String apellido = consola.leerTexto("Filtrar por apellido (Enter = todos): ", false, Persona.MAX_NOMBRE);

        TablaConsola tabla = new TablaConsola("Jugador", "Categoría", "Estado");
        int enTratamiento = 0;
        int mostradas = 0;
        for (EstadoJugadorDTO fila : filas) {
            boolean cumpleCategoria = categoria.isEmpty() || categoria.get().equals(fila.categoria());
            boolean cumpleApellido = apellido.isEmpty() || Texto.contiene(apellidoDe(fila), apellido);
            if (cumpleCategoria && cumpleApellido) {
                tabla.agregarFila(fila.jugador(), fila.categoria(), fila.estado().getEtiqueta());
                mostradas++;
                if (fila.estado() == EstadoDisponibilidad.EN_TRATAMIENTO) {
                    enTratamiento++;
                }
            }
        }
        if (tabla.estaVacia()) {
            consola.mostrarAviso("No se encontraron resultados para los filtros seleccionados.");
        } else {
            tabla.imprimir(consola);
            consola.mostrar("Disponibles: " + (mostradas - enTratamiento) + " | En tratamiento: " + enTratamiento);
        }
        consola.mostrar("Vista de solo lectura. No se muestran lesiones, tratamientos, evoluciones ni "
                + "diagnósticos (RF12, RNF13).");
    }

    /** Categorías de las filas, sin repetir y en el orden en que aparecen. */
    private static List<String> categoriasPresentes(List<EstadoJugadorDTO> filas) {
        List<String> categorias = new ArrayList<>();
        for (EstadoJugadorDTO fila : filas) {
            if (!categorias.contains(fila.categoria())) {
                categorias.add(fila.categoria());
            }
        }
        return categorias;
    }

    /** El DTO trae "Apellido, Nombre": el apellido es lo anterior a la coma. */
    private static String apellidoDe(EstadoJugadorDTO fila) {
        int coma = fila.jugador().indexOf(',');
        return coma < 0 ? fila.jugador() : fila.jugador().substring(0, coma);
    }
}
