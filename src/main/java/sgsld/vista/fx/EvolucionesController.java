package sgsld.vista.fx;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import sgsld.controlador.Formatos;
import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.Evolucion;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.Tratamiento;
import sgsld.servicio.TratamientoService;
import sgsld.util.Fechas;
import sgsld.util.Texto;

/**
 * Evoluciones (CU-06, {@code evoluciones.fxml}). Rol: Kinesiólogo.
 * <p>
 * Muestra los tratamientos en curso y las evoluciones del elegido. Permite
 * registrar una evolución (el tratamiento sigue en curso) o "Registrar y
 * finalizar": tras confirmar la fecha de fin (se propone la de la evolución),
 * el servicio guarda la evolución y la finalización en una sola transacción y
 * se informan la duración y el nuevo estado del jugador. Si se cancela la
 * confirmación, no se guarda nada.
 */
public class EvolucionesController extends ControladorPantalla {

    /** Encabezados de los tratamientos en curso (cantidad fija). */
    private static final String[] EN_CURSO = {"N°", "Jugador", "Lesión", "Responsable", "Inicio", "Días",
        "Evoluciones"};
    /** Encabezados de las evoluciones (cantidad fija). */
    private static final String[] EVOLUCIONES = {"Fecha", "Resultado", "Observación"};

    @FXML
    private TableView<Tratamiento> tablaTratamientos;
    @FXML
    private Label tituloEvoluciones;
    @FXML
    private TableView<Evolucion> tablaEvoluciones;
    @FXML
    private DatePicker campoFecha;
    @FXML
    private ComboBox<ResultadoEvolucion> comboResultado;
    @FXML
    private TextField campoObservacion;
    @FXML
    private Button botonRegistrar;
    @FXML
    private Button botonFinalizar;
    @FXML
    private Label etiquetaResultado;

    /** Tratamiento finalizado y nuevo estado del jugador, leídos en la misma tarea. */
    private record Finalizacion(Tratamiento tratamiento, EstadoDisponibilidad estado) {
    }

    @FXML
    private void initialize() {
        tablaTratamientos.getColumns().add(Tablas.columnaNumerica(EN_CURSO[0], t -> String.valueOf(t.getId())));
        tablaTratamientos.getColumns().add(Tablas.columna(EN_CURSO[1],
                t -> t.getLesion().getJugador().getNombreCompleto()));
        tablaTratamientos.getColumns().add(Tablas.columna(EN_CURSO[2], t -> t.getLesion().getZona().getNombre() + " "
                + Fechas.formatear(t.getLesion().getFecha())));
        tablaTratamientos.getColumns().add(Tablas.columna(EN_CURSO[3], t -> t.getKinesiologo().getNombreCompleto()));
        tablaTratamientos.getColumns().add(Tablas.columna(EN_CURSO[4], t -> Fechas.formatear(t.getFechaInicio())));
        tablaTratamientos.getColumns().add(Tablas.columnaNumerica(EN_CURSO[5],
                t -> String.valueOf(t.getDiasTranscurridos(hoy()))));
        tablaTratamientos.getColumns().add(Tablas.columnaNumerica(EN_CURSO[6],
                t -> String.valueOf(t.getCantidadEvoluciones())));
        Tablas.distribuirAnchos(tablaTratamientos, 45, 145, 185, 145, 100, 60, 110);
        Tablas.ajustarAltura(tablaTratamientos, 1, 4, Tablas.ALTO_FILA_COMPACTA);
        tablaTratamientos.setPlaceholder(new Label("No hay tratamientos en curso."));
        tablaTratamientos.getSelectionModel().selectedItemProperty().addListener(
                (propiedad, anterior, tratamiento) -> mostrarEvoluciones(tratamiento));

        tablaEvoluciones.getColumns().add(Tablas.columna(EVOLUCIONES[0], e -> Fechas.formatear(e.getFecha())));
        tablaEvoluciones.getColumns().add(Tablas.columna(EVOLUCIONES[1], e -> e.getResultado().getEtiqueta()));
        tablaEvoluciones.getColumns().add(Tablas.columna(EVOLUCIONES[2], Evolucion::getObservacion));
        Tablas.distribuirAnchos(tablaEvoluciones, 100, 110, 580);
        Tablas.ajustarAltura(tablaEvoluciones, 1, 4, Tablas.ALTO_FILA_COMPACTA);

        Campos.configurarFecha(campoFecha, hoy());
        Campos.mostrarCon(comboResultado, ResultadoEvolucion::getEtiqueta);
        comboResultado.getItems().setAll(ResultadoEvolucion.values());
        botonRegistrar.disableProperty().bind(ocupadoProperty());
        botonFinalizar.disableProperty().bind(ocupadoProperty());
        etiquetaResultado.managedProperty().bind(etiquetaResultado.visibleProperty());
        mostrarEvoluciones(null);
        limpiar();
    }

    @Override
    public String getTitulo() {
        return "Evoluciones";
    }

    @Override
    public void alMostrar() {
        cargar(null);
    }

    /** Lee los tratamientos en curso y selecciona el indicado (o el primero). */
    private void cargar(Integer idASeleccionar) {
        ejecutar(() -> servicios().tratamientos().listarEnCurso(), enCurso -> {
            tablaTratamientos.getItems().setAll(enCurso);
            Tratamiento elegido = enCurso.isEmpty() ? null : enCurso.get(0);
            for (Tratamiento tratamiento : enCurso) {
                if (idASeleccionar != null && tratamiento.getId() == idASeleccionar) {
                    elegido = tratamiento;
                }
            }
            if (elegido == null) {
                mostrarEvoluciones(null);
            } else {
                tablaTratamientos.getSelectionModel().select(elegido);
                tablaTratamientos.scrollTo(elegido);
                // Si era el mismo objeto, la selección no cambia: se actualizan las evoluciones igual.
                mostrarEvoluciones(elegido);
            }
        });
    }

    private void mostrarEvoluciones(Tratamiento tratamiento) {
        if (tratamiento == null) {
            tituloEvoluciones.setText("EVOLUCIONES");
            tablaEvoluciones.getItems().clear();
            tablaEvoluciones.setPlaceholder(new Label("Elija un tratamiento en curso."));
            return;
        }
        tituloEvoluciones.setText(("Evoluciones del tratamiento N° " + tratamiento.getId() + " · "
                + tratamiento.getLesion().getJugador().getNombreCompleto() + " · "
                + Formatos.lesion(tratamiento.getLesion())).toUpperCase(Texto.LOCALE));
        tablaEvoluciones.setPlaceholder(new Label("Sin evoluciones registradas."));
        tablaEvoluciones.getItems().setAll(tratamiento.getEvoluciones());
    }

    /** Registra la evolución; el tratamiento sigue en curso. */
    @FXML
    private void registrar() {
        Tratamiento tratamiento = tablaTratamientos.getSelectionModel().getSelectedItem();
        List<String> errores = new ArrayList<>();
        Evolucion evolucion = armarEvolucion(tratamiento, errores);
        if (!errores.isEmpty()) {
            mostrarErrores(errores);
            return;
        }
        int id = tratamiento.getId();
        etiquetaResultado.setVisible(false);
        ejecutar(() -> servicios().tratamientos().registrarEvolucion(id, evolucion), idEvolucion -> {
            informarResultado("Evolución N° " + idEvolucion + " registrada. El tratamiento N° " + id
                    + " sigue En curso.");
            limpiar();
            cargar(id);
        });
    }

    /**
     * Registrar y finalizar: confirma la fecha de fin (propone la de la
     * evolución) y guarda las dos cosas en una sola transacción.
     */
    @FXML
    private void registrarYFinalizar() {
        Tratamiento tratamiento = tablaTratamientos.getSelectionModel().getSelectedItem();
        List<String> errores = new ArrayList<>();
        Evolucion evolucion = armarEvolucion(tratamiento, errores);
        if (!errores.isEmpty()) {
            mostrarErrores(errores);
            return;
        }
        LocalDate propuesta = evolucion.getFecha() == null ? hoy() : evolucion.getFecha();
        DialogoFinalizacion confirmacion = new DialogoFinalizacion(ventana(), tratamiento, propuesta, hoy());
        if (presentar(confirmacion.dialogo()).filter(DialogoFinalizacion.FINALIZAR::equals).isEmpty()) {
            informarResultado("No se guardó nada: el tratamiento N° " + tratamiento.getId() + " sigue En curso.");
            return;
        }
        LocalDate fin = confirmacion.leerFechaFin(errores);
        if (!errores.isEmpty()) {
            mostrarErrores(errores);
            return;
        }
        int id = tratamiento.getId();
        int idJugador = tratamiento.getLesion().getJugador().getId();
        etiquetaResultado.setVisible(false);
        TratamientoService tratamientos = servicios().tratamientos();
        ejecutar(() -> {
            tratamientos.registrarEvolucionYFinalizar(id, evolucion, fin);
            return new Finalizacion(tratamientos.buscarPorId(id), servicios().consultas().consultarEstado(idJugador));
        }, finalizacion -> {
            Tratamiento finalizado = finalizacion.tratamiento();
            String jugador = finalizado.getLesion().getJugador().getNombreCompleto();
            informarResultado("Tratamiento N° " + id + " finalizado.");
            limpiar();
            cargar(null);
            mostrarInfo("Se registró la evolución y se finalizó el tratamiento N° " + id + ".\n\n"
                    + "Fecha de fin: " + Fechas.formatear(finalizado.getFechaFin()) + "\n"
                    + "Duración: " + Formatos.dias(finalizado.getDuracionDias()) + "\n"
                    + "Estado actual de " + jugador + ": " + finalizacion.estado() + ".");
        });
    }

    /** Arma la evolución con lo escrito; los errores de formato se agregan a la lista. */
    private Evolucion armarEvolucion(Tratamiento tratamiento, List<String> errores) {
        if (tratamiento == null) {
            errores.add("Debe seleccionar un tratamiento en curso en la tabla.");
        }
        LocalDate fecha = Campos.leerFecha(campoFecha, "fecha de la evolución", errores);
        String observacion = Campos.leerTexto(campoObservacion);
        return new Evolucion(fecha, observacion.isEmpty() ? null : observacion, comboResultado.getValue());
    }

    private void informarResultado(String mensaje) {
        etiquetaResultado.setText(mensaje);
        etiquetaResultado.setVisible(true);
    }

    /** Deja la nueva evolución vacía, con fecha de hoy. */
    @FXML
    private void limpiar() {
        Campos.mostrarFecha(campoFecha, hoy());
        comboResultado.setValue(null);
        campoObservacion.clear();
    }
}
