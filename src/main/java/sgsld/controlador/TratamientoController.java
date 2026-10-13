package sgsld.controlador;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Lesion;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.Usuario;
import sgsld.servicio.ConsultaService;
import sgsld.servicio.KinesiologoService;
import sgsld.servicio.LesionService;
import sgsld.servicio.TratamientoService;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.Fechas;
import sgsld.util.SesionUsuario;
import sgsld.util.ValidacionException;
import sgsld.vista.consola.Consola;
import sgsld.vista.consola.TablaConsola;

/**
 * Tratamientos (CU-05) y evoluciones con finalización (CU-06).
 */
public class TratamientoController {

    private final Consola consola;
    private final TratamientoService tratamientos;
    private final LesionService lesiones;
    private final KinesiologoService kinesiologos;
    private final ConsultaService consultas;
    private final SelectorJugador selector;

    /**
     * Crea el controlador.
     *
     * @param consola      entrada y salida
     * @param tratamientos servicio de tratamientos
     * @param lesiones     servicio de lesiones
     * @param kinesiologos servicio de kinesiólogos
     * @param consultas    servicio de consultas (estado del jugador)
     * @param selector     selección de jugadores
     */
    public TratamientoController(Consola consola, TratamientoService tratamientos, LesionService lesiones,
                                 KinesiologoService kinesiologos, ConsultaService consultas, SelectorJugador selector) {
        this.consola = consola;
        this.tratamientos = tratamientos;
        this.lesiones = lesiones;
        this.kinesiologos = kinesiologos;
        this.consultas = consultas;
        this.selector = selector;
    }

    /**
     * Registra un tratamiento eligiendo primero el jugador y una de sus lesiones.
     *
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     */
    public void registrarTratamiento() throws AccesoDenegadoException {
        consola.titulo("Registro de tratamiento");
        Optional<Jugador> jugador = selector.seleccionar(true);
        if (jugador.isEmpty()) {
            return;
        }
        List<Lesion> historial;
        try {
            historial = lesiones.consultarHistorial(jugador.get().getId());
        } catch (EntidadNoEncontradaException e) {
            consola.mostrarError(e.getMessage());
            return;
        }
        if (historial.isEmpty()) {
            consola.mostrarAviso("El jugador no tiene lesiones registradas. Registre primero la lesión.");
            return;
        }
        Optional<Lesion> lesion = consola.elegirDeLista("Lesiones de " + jugador.get().getNombreCompleto() + ":",
                historial, l -> "N° " + l.getId() + " - " + Formatos.lesion(l) + " - " + Formatos.estadoDeLesion(l));
        if (lesion.isPresent()) {
            registrarTratamiento(lesion.get());
        }
    }

    /**
     * Registra un tratamiento para una lesión ya elegida. Propone como
     * responsable al kinesiólogo de la sesión. El tratamiento queda "En curso".
     *
     * @param lesion lesión a tratar
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     */
    public void registrarTratamiento(Lesion lesion) throws AccesoDenegadoException {
        consola.mostrar("Tratamiento para la lesión N° " + lesion.getId() + ": " + Formatos.lesion(lesion));
        Optional<Kinesiologo> responsable = elegirResponsable();
        if (responsable.isEmpty()) {
            consola.mostrar("Operación cancelada.");
            return;
        }
        Tratamiento base = null;
        while (true) {
            LocalDate inicio = consola.leerFecha("Fecha de inicio ",
                    base == null ? tratamientos.hoy() : base.getFechaInicio());
            String tipo = consola.editarTexto("Tipo de tratamiento", base == null ? null : base.getTipo(), true,
                    Tratamiento.MAX_TIPO);
            String observaciones = consola.editarTexto("Observaciones (opcional)",
                    base == null ? null : base.getObservaciones(), false, Tratamiento.MAX_OBSERVACIONES);
            Tratamiento nuevo = new Tratamiento(lesion, responsable.get(), inicio, tipo,
                    observaciones.isEmpty() ? null : observaciones);
            try {
                int id = tratamientos.registrarTratamiento(nuevo);
                consola.mostrarOk("Tratamiento N° " + id + " registrado. Estado: " + nuevo.getEstado() + ".");
                return;
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                if (lesion.tieneTratamientoEnCurso()
                        || !consola.leerSiNo("¿Desea corregir los datos? (S/N): ")) {
                    consola.mostrar("No se registró el tratamiento.");
                    return;
                }
                base = nuevo;
            }
        }
    }

    /**
     * Registra una evolución de un tratamiento en curso y ofrece finalizarlo
     * (CU-06). Si la finalización no se confirma, sigue en curso (6a).
     *
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     */
    public void registrarEvolucion() throws AccesoDenegadoException {
        consola.titulo("Registro de evolución");
        List<Tratamiento> enCurso = tratamientos.listarEnCurso();
        if (enCurso.isEmpty()) {
            consola.mostrarAviso("No hay tratamientos en curso.");
            return;
        }
        mostrarTratamientosEnCurso(enCurso);
        Optional<Tratamiento> elegido = elegirTratamiento();
        if (elegido.isEmpty()) {
            return;
        }
        Tratamiento tratamiento = elegido.get();
        mostrarEvoluciones(tratamiento);
        Optional<Evolucion> registrada = pedirEvolucion(tratamiento);
        if (registrada.isPresent()
                && consola.leerSiNo("¿Finalizar el tratamiento? (S/N): ")) {
            finalizar(tratamiento, registrada.get().getFecha());
        } else if (registrada.isPresent()) {
            consola.mostrar("El tratamiento N° " + tratamiento.getId() + " sigue En curso.");
        }
    }

    private Optional<Kinesiologo> elegirResponsable() throws AccesoDenegadoException {
        Usuario usuario = SesionUsuario.getInstancia().getUsuario();
        Optional<Kinesiologo> propio = kinesiologos.buscarPorUsuario(usuario.getId());
        if (propio.isPresent() && consola.leerSiNo("Responsable: " + propio.get().getNombreCompleto() + " ("
                + propio.get().getIdentificacion() + "). ¿Mantener? (S/N): ")) {
            return propio;
        }
        return consola.elegirDeLista("Kinesiólogo responsable:", kinesiologos.listar(),
                k -> k.getNombreCompleto() + " (" + k.getIdentificacion() + ")");
    }

    private void mostrarTratamientosEnCurso(List<Tratamiento> enCurso) {
        TablaConsola tabla = new TablaConsola("N°", "Jugador", "Lesión", "Responsable", "Inicio", "Evoluciones")
                .alinearDerecha(0, 5);
        for (Tratamiento tratamiento : enCurso) {
            Lesion lesion = tratamiento.getLesion();
            tabla.agregarFila(String.valueOf(tratamiento.getId()), lesion.getJugador().getNombreCompleto(),
                    lesion.getZona().getNombre() + " " + Fechas.formatear(lesion.getFecha()),
                    tratamiento.getKinesiologo().getNombreCompleto(), Fechas.formatear(tratamiento.getFechaInicio()),
                    String.valueOf(tratamiento.getCantidadEvoluciones()));
        }
        tabla.imprimir(consola);
    }

    /** Pide el número de tratamiento hasta que corresponda a uno en curso (0 cancela). */
    private Optional<Tratamiento> elegirTratamiento() throws AccesoDenegadoException {
        while (true) {
            int numero = consola.leerEntero("N° de tratamiento (0 = cancelar): ");
            if (numero == 0) {
                return Optional.empty();
            }
            try {
                Tratamiento tratamiento = tratamientos.buscarPorId(numero);
                if (tratamiento.estaEnCurso()) {
                    return Optional.of(tratamiento);
                }
                consola.mostrarError("El tratamiento N° " + numero + " ya está finalizado.");
            } catch (EntidadNoEncontradaException e) {
                consola.mostrarError(e.getMessage());
            }
        }
    }

    private void mostrarEvoluciones(Tratamiento tratamiento) {
        Lesion lesion = tratamiento.getLesion();
        consola.mostrar("Tratamiento N° " + tratamiento.getId() + ": " + tratamiento.getTipo() + " - "
                + lesion.getJugador().getNombreCompleto() + " - " + Formatos.lesion(lesion));
        if (tratamiento.getEvoluciones().isEmpty()) {
            consola.mostrar("Sin evoluciones registradas.");
            return;
        }
        TablaConsola tabla = new TablaConsola("Fecha", "Resultado", "Observación");
        for (Evolucion evolucion : tratamiento.getEvoluciones()) {
            tabla.agregarFila(Fechas.formatear(evolucion.getFecha()), evolucion.getResultado().getEtiqueta(),
                    evolucion.getObservacion());
        }
        tabla.imprimir(consola);
    }

    private Optional<Evolucion> pedirEvolucion(Tratamiento tratamiento) throws AccesoDenegadoException {
        Evolucion base = null;
        while (true) {
            LocalDate fecha = consola.leerFecha("Fecha de la evolución ",
                    base == null ? tratamientos.hoy() : base.getFecha());
            String observacion = consola.editarTexto("Observación", base == null ? null : base.getObservacion(),
                    true, Evolucion.MAX_OBSERVACION);
            Optional<ResultadoEvolucion> resultado = consola.elegirDeLista("Resultado:",
                    List.of(ResultadoEvolucion.values()), ResultadoEvolucion::getEtiqueta);
            if (resultado.isEmpty()) {
                consola.mostrar("Operación cancelada.");
                return Optional.empty();
            }
            Evolucion evolucion = new Evolucion(fecha, observacion, resultado.get());
            try {
                int id = tratamientos.registrarEvolucion(tratamiento.getId(), evolucion);
                consola.mostrarOk("Evolución N° " + id + " registrada.");
                return Optional.of(evolucion);
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                if (!consola.leerSiNo("¿Desea corregir los datos? (S/N): ")) {
                    consola.mostrar("No se registró la evolución.");
                    return Optional.empty();
                }
                base = evolucion;
            } catch (EntidadNoEncontradaException e) {
                consola.mostrarError(e.getMessage());
                return Optional.empty();
            }
        }
    }

    private void finalizar(Tratamiento tratamiento, LocalDate fechaEvolucion) throws AccesoDenegadoException {
        LocalDate fin = consola.leerFecha("Fecha de fin ", fechaEvolucion);
        if (!consola.leerSiNo("¿Confirma la finalización del tratamiento N° " + tratamiento.getId() + " con fecha "
                + Fechas.formatear(fin) + "? (S/N): ")) {
            consola.mostrar("Finalización cancelada: el tratamiento sigue En curso.");
            return;
        }
        try {
            tratamientos.finalizarTratamiento(tratamiento.getId(), fin);
            Jugador jugador = tratamiento.getLesion().getJugador();
            EstadoDisponibilidad estado = consultas.consultarEstado(jugador.getId());
            consola.mostrarOk("Tratamiento N° " + tratamiento.getId() + " finalizado. Duración: "
                    + Formatos.dias(tratamiento.getDuracionDias()) + ".");
            consola.mostrar("Estado actual de " + jugador.getNombreCompleto() + ": " + estado + ".");
        } catch (ValidacionException e) {
            consola.mostrarErrores(e.getErrores());
            consola.mostrar("El tratamiento sigue En curso.");
        } catch (EntidadNoEncontradaException e) {
            consola.mostrarError(e.getMessage());
        }
    }
}
