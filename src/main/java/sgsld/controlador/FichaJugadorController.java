package sgsld.controlador;

import java.time.LocalDate;
import java.util.Optional;

import sgsld.modelo.Evolucion;
import sgsld.modelo.Jugador;
import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.dto.FichaJugadorDTO;
import sgsld.servicio.ConsultaService;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.Fechas;
import sgsld.vista.consola.Consola;
import sgsld.vista.consola.TablaConsola;

/**
 * Ficha individual del jugador (CU-10): datos básicos, estado actual e
 * historial; luego, el detalle de tratamientos y evoluciones de cada lesión.
 */
public class FichaJugadorController {

    private final Consola consola;
    private final ConsultaService consultas;
    private final SelectorJugador selector;

    /**
     * Crea el controlador.
     *
     * @param consola   entrada y salida
     * @param consultas servicio de consultas
     * @param selector  selección de jugadores
     */
    public FichaJugadorController(Consola consola, ConsultaService consultas, SelectorJugador selector) {
        this.consola = consola;
        this.consultas = consultas;
        this.selector = selector;
    }

    /**
     * Busca un jugador y muestra su ficha. Si no hay coincidencias, informa
     * "No se encontraron resultados" (alternativo 2a).
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador ni Kinesiólogo
     */
    public void consultarFicha() throws AccesoDenegadoException {
        consola.titulo("Ficha individual del jugador");
        Optional<Jugador> elegido = selector.seleccionar(false);
        if (elegido.isEmpty()) {
            return;
        }
        FichaJugadorDTO ficha;
        try {
            ficha = consultas.consultarFicha(elegido.get().getId());
        } catch (EntidadNoEncontradaException e) {
            consola.mostrarError(e.getMessage());
            return;
        }
        mostrarDatosBasicos(ficha);
        mostrarHistorial(ficha);
        while (!ficha.historial().isEmpty()) {
            int numero = consola.leerEntero("N° de lesión para ver tratamientos y evoluciones (0 = volver): ");
            if (numero == 0) {
                return;
            }
            Optional<Lesion> lesion = buscarEnHistorial(ficha, numero);
            if (lesion.isPresent()) {
                mostrarDetalle(lesion.get());
            } else {
                consola.mostrarError("La lesión N° " + numero + " no pertenece al historial del jugador.");
            }
        }
    }

    private void mostrarDatosBasicos(FichaJugadorDTO ficha) {
        Jugador jugador = ficha.jugador();
        LocalDate hoy = consultas.hoy();
        consola.titulo("Ficha de " + jugador.getNombreCompleto());
        consola.mostrar("DNI:                 " + jugador.getDni());
        consola.mostrar("Nombre:              " + jugador.getNombre() + " " + jugador.getApellido());
        consola.mostrar("Fecha de nacimiento: " + Fechas.formatear(jugador.getFechaNacimiento())
                + " (" + jugador.getEdad(hoy) + " años)");
        consola.mostrar("Posición:            " + jugador.getPosicion());
        consola.mostrar("Camiseta:            "
                + (jugador.getNumeroCamiseta() == null ? "sin número" : jugador.getNumeroCamiseta()));
        consola.mostrar("Categoría vigente:   " + jugador.getCategoria().getNombre()
                + " (" + jugador.getCategoria().getRangoEtario() + ")");
        consola.mostrar("Activo:              " + (jugador.isActivo() ? "Sí" : "No (dado de baja)"));
        consola.mostrar("Estado actual:       " + ficha.estado());
    }

    private void mostrarHistorial(FichaJugadorDTO ficha) {
        consola.titulo("Historial de lesiones");
        if (ficha.historial().isEmpty()) {
            consola.mostrar("Sin lesiones registradas.");
            return;
        }
        TablaConsola tabla = new TablaConsola("N°", "Fecha", "Zona", "Tipo", "Gravedad", "Estado").alinearDerecha(0);
        for (Lesion lesion : ficha.historial()) {
            tabla.agregarFila(String.valueOf(lesion.getId()), Fechas.formatear(lesion.getFecha()),
                    lesion.getZona().getNombre(), lesion.getTipo().getNombre(), lesion.getGravedad().getEtiqueta(),
                    Formatos.estadoDeLesion(lesion));
        }
        tabla.imprimir(consola);
    }

    /** Búsqueda secuencial del número de lesión dentro del historial del jugador. */
    private static Optional<Lesion> buscarEnHistorial(FichaJugadorDTO ficha, int numero) {
        for (Lesion lesion : ficha.historial()) {
            if (lesion.getId() == numero) {
                return Optional.of(lesion);
            }
        }
        return Optional.empty();
    }

    private void mostrarDetalle(Lesion lesion) {
        consola.titulo("Lesión N° " + lesion.getId() + ": " + Formatos.lesion(lesion));
        consola.mostrar("Descripción: " + (lesion.getDescripcion() == null ? "-" : lesion.getDescripcion()));
        if (lesion.getTratamientos().isEmpty()) {
            consola.mostrar("Sin tratamientos registrados.");
            return;
        }
        for (Tratamiento tratamiento : lesion.getTratamientos()) {
            consola.lineaEnBlanco();
            consola.mostrar("Tratamiento N° " + tratamiento.getId() + ": " + tratamiento.getTipo());
            consola.mostrar("  Responsable:   " + tratamiento.getKinesiologo().getNombreCompleto()
                    + " (" + tratamiento.getKinesiologo().getIdentificacion() + ")");
            consola.mostrar("  Inicio:        " + Fechas.formatear(tratamiento.getFechaInicio())
                    + "   Fin: " + Fechas.formatear(tratamiento.getFechaFin()));
            consola.mostrar("  Estado:        " + tratamiento.getEstado() + "   Duración: " + duracion(tratamiento));
            consola.mostrar("  Observaciones: "
                    + (tratamiento.getObservaciones() == null ? "-" : tratamiento.getObservaciones()));
            if (tratamiento.getEvoluciones().isEmpty()) {
                consola.mostrar("  Sin evoluciones registradas.");
            } else {
                TablaConsola tabla = new TablaConsola("Fecha", "Resultado", "Observación");
                for (Evolucion evolucion : tratamiento.getEvoluciones()) {
                    tabla.agregarFila(Fechas.formatear(evolucion.getFecha()), evolucion.getResultado().getEtiqueta(),
                            evolucion.getObservacion());
                }
                tabla.imprimir(consola);
            }
        }
    }

    private String duracion(Tratamiento tratamiento) {
        return tratamiento.estaEnCurso()
                ? Formatos.dias(tratamiento.getDiasTranscurridos(consultas.hoy())) + " transcurridos"
                : Formatos.dias(tratamiento.getDuracionDias());
    }
}
