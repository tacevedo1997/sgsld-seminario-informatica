package sgsld.controlador;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import sgsld.modelo.Gravedad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Lesion;
import sgsld.modelo.TipoLesion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.ZonaCorporal;
import sgsld.servicio.LesionService;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.Fechas;
import sgsld.util.ValidacionException;
import sgsld.vista.consola.Consola;
import sgsld.vista.consola.TablaConsola;

/**
 * Registro de lesiones (CU-04). Incluye la consulta del historial (CU-12) y se
 * extiende con la alerta de recurrencia (CU-11). Al terminar, permite
 * registrar el tratamiento en el mismo flujo (RNF05).
 */
public class RegistroLesionController {

    private final Consola consola;
    private final LesionService lesiones;
    private final SelectorJugador selector;
    private final TratamientoController tratamientos;

    /** Fecha de la lesión que se está registrando; la usa {@link #confirmarAlerta(Lesion)}. */
    private LocalDate fechaLesionEnRegistro;

    /**
     * Crea el controlador.
     *
     * @param consola      entrada y salida
     * @param lesiones     servicio de lesiones
     * @param selector     selección de jugadores
     * @param tratamientos controlador de tratamientos, para continuar con CU-05
     */
    public RegistroLesionController(Consola consola, LesionService lesiones, SelectorJugador selector,
                                    TratamientoController tratamientos) {
        this.consola = consola;
        this.lesiones = lesiones;
        this.selector = selector;
        this.tratamientos = tratamientos;
    }

    /**
     * Flujo completo de registro de una lesión.
     *
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     */
    public void registrarLesion() throws AccesoDenegadoException {
        consola.titulo("Registro de lesión");
        Optional<Jugador> elegido = selector.seleccionar(true);
        if (elegido.isEmpty()) {
            return;
        }
        Jugador jugador = elegido.get();
        mostrarHistorial(jugador);
        List<TipoLesion> tipos = lesiones.listarTipos();
        List<ZonaCorporal> zonas = lesiones.listarZonas();
        Lesion base = null;
        while (true) {
            Optional<Lesion> candidata = pedirDatos(jugador, tipos, zonas, base);
            if (candidata.isEmpty()) {
                consola.mostrar("Operación cancelada.");
                return;
            }
            Lesion nueva = candidata.get();
            try {
                lesiones.validar(nueva);
                List<Lesion> antecedentes = lesiones.buscarAntecedentes(nueva);
                Optional<Lesion> antecedente = lesiones.verificarRecurrencia(nueva, antecedentes);
                if (antecedente.isPresent()) {
                    fechaLesionEnRegistro = nueva.getFecha();
                    confirmarAlerta(antecedente.get());
                }
                int id = lesiones.registrarLesion(nueva);
                consola.mostrarOk("Lesión registrada con el N° " + id + ".");
                if (consola.leerSiNo("¿Registrar el tratamiento ahora? (S/N): ")) {
                    tratamientos.registrarTratamiento(nueva);
                }
                return;
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                if (!consola.leerSiNo("¿Desea corregir los datos? (S/N): ")) {
                    consola.mostrar("Operación cancelada: no se registró la lesión.");
                    return;
                }
                base = nueva;
            }
        }
    }

    /**
     * Muestra la alerta de posible lesión recurrente (CU-11, Figura 11) y espera
     * que el kinesiólogo confirme haberla visto. La alerta es informativa: no
     * impide continuar con el registro.
     *
     * @param antecedente lesión anterior que activó la alerta
     * @return {@code true} cuando el kinesiólogo confirmó haber visto la alerta
     */
    public boolean confirmarAlerta(Lesion antecedente) {
        Tratamiento ultimo = antecedente.getUltimoTratamiento();
        String encabezado = antecedente.getZona().getNombre() + " - " + Fechas.formatear(antecedente.getFecha());
        consola.lineaEnBlanco();
        consola.mostrar("[ALERTA] Posible lesión recurrente");
        consola.mostrar("El jugador registra una lesión anterior en la misma zona corporal:");
        if (ultimo == null) {
            consola.mostrar(encabezado + ", sin tratamiento registrado");
        } else if (ultimo.estaEnCurso()) {
            consola.mostrar(encabezado + ", tratamiento en curso");
        } else {
            LocalDate fin = ultimo.getFechaFin();
            consola.mostrar(encabezado + ", tratamiento finalizado el " + Fechas.formatear(fin));
            consola.mostrar(fin.isAfter(fechaLesionEnRegistro)
                    ? "(" + Fechas.describirPeriodo(fechaLesionEnRegistro, fin) + " después de la nueva lesión)."
                    : "(" + Fechas.describirPeriodo(fin, fechaLesionEnRegistro) + " antes de la nueva lesión).");
        }
        consola.mostrar("La alerta es informativa y no constituye un diagnóstico médico. "
                + "Puede continuar con el registro.");
        consola.pausa();
        return true;
    }

    /** Historial del jugador, que se muestra automáticamente al elegirlo (RNF06). */
    private void mostrarHistorial(Jugador jugador) throws AccesoDenegadoException {
        consola.titulo("Historial de " + jugador.getNombreCompleto());
        try {
            List<Lesion> historial = lesiones.consultarHistorial(jugador.getId());
            if (historial.isEmpty()) {
                consola.mostrar("Sin lesiones registradas.");
                return;
            }
            TablaConsola tabla = new TablaConsola("N°", "Fecha", "Zona", "Tipo", "Gravedad", "Estado")
                    .alinearDerecha(0);
            for (Lesion lesion : historial) {
                tabla.agregarFila(String.valueOf(lesion.getId()), Fechas.formatear(lesion.getFecha()),
                        lesion.getZona().getNombre(), lesion.getTipo().getNombre(), lesion.getGravedad().getEtiqueta(),
                        Formatos.estadoDeLesion(lesion));
            }
            tabla.imprimir(consola);
        } catch (EntidadNoEncontradaException e) {
            consola.mostrarError(e.getMessage());
        }
    }

    /**
     * Pide tipo, zona, gravedad, fecha y descripción. En una corrección, 0
     * conserva la selección anterior y Enter conserva cada texto.
     *
     * @return la lesión candidata, o vacío si se canceló una selección en el primer intento
     */
    private Optional<Lesion> pedirDatos(Jugador jugador, List<TipoLesion> tipos, List<ZonaCorporal> zonas,
                                        Lesion base) {
        boolean hayBase = base != null;
        if (hayBase) {
            consola.mostrar("En cada lista, 0 conserva el valor anterior.");
        }
        Optional<TipoLesion> tipo = consola.elegirDeLista("Tipo de lesión:", tipos, TipoLesion::getNombre);
        if (tipo.isEmpty() && !hayBase) {
            return Optional.empty();
        }
        Optional<ZonaCorporal> zona = consola.elegirDeLista("Zona corporal:", zonas,
                z -> z.getNombre() + " (" + z.getRegion() + ")");
        if (zona.isEmpty() && !hayBase) {
            return Optional.empty();
        }
        Optional<Gravedad> gravedad = consola.elegirDeLista("Gravedad:", List.of(Gravedad.values()),
                Gravedad::getEtiqueta);
        if (gravedad.isEmpty() && !hayBase) {
            return Optional.empty();
        }
        LocalDate fecha = consola.leerFecha("Fecha de la lesión ", hayBase ? base.getFecha() : lesiones.hoy());
        String descripcion = consola.editarTexto("Descripción (opcional)", hayBase ? base.getDescripcion() : null,
                false, Lesion.MAX_DESCRIPCION);
        return Optional.of(new Lesion(jugador,
                tipo.isPresent() ? tipo.get() : base.getTipo(),
                zona.isPresent() ? zona.get() : base.getZona(),
                gravedad.isPresent() ? gravedad.get() : base.getGravedad(),
                fecha, descripcion.isEmpty() ? null : descripcion));
    }
}
