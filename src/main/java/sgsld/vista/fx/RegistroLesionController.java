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
import javafx.scene.control.TextArea;

import sgsld.modelo.Gravedad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Lesion;
import sgsld.modelo.TipoLesion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.ZonaCorporal;
import sgsld.servicio.LesionService;
import sgsld.util.Fechas;
import sgsld.util.Texto;

/**
 * Registrar lesión (CU-04, Figura 11, {@code registro_lesion.fxml}). Rol: Kinesiólogo.
 * <p>
 * Al elegir el jugador se muestra su historial (CU-12). Al registrar, el
 * servicio valida los datos y verifica la regla de recurrencia (CU-11): si hay
 * un antecedente, se muestra la alerta informativa y luego se guarda. Al
 * final se ofrece registrar el tratamiento en la pantalla Tratamientos, con
 * la lesión ya elegida.
 */
public class RegistroLesionController extends ControladorPantalla {

    /** Encabezados del historial (cantidad fija, como en la Figura 11). */
    private static final String[] HISTORIAL = {"Fecha", "Zona corporal", "Tipo", "Gravedad", "Tratamiento", "Estado",
        "Fin"};

    @FXML
    private ComboBox<Jugador> comboJugador;
    @FXML
    private TableView<Lesion> tablaHistorial;
    @FXML
    private ComboBox<TipoLesion> comboTipo;
    @FXML
    private ComboBox<ZonaCorporal> comboZona;
    @FXML
    private ComboBox<Gravedad> comboGravedad;
    @FXML
    private DatePicker campoFecha;
    @FXML
    private TextArea campoDescripcion;
    @FXML
    private Button botonRegistrar;
    @FXML
    private Label etiquetaResultado;

    /** Jugadores y catálogos leídos en una misma tarea. */
    private record Catalogos(List<Jugador> jugadores, List<TipoLesion> tipos, List<ZonaCorporal> zonas) {
    }

    @FXML
    private void initialize() {
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[0], lesion -> Fechas.formatear(lesion.getFecha())));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[1], lesion -> lesion.getZona().getNombre()));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[2], lesion -> lesion.getTipo().getNombre()));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[3], lesion -> lesion.getGravedad().getEtiqueta()));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[4], lesion -> lesion.getUltimoTratamiento() == null
                ? "Sin tratamiento" : lesion.getUltimoTratamiento().getTipo()));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[5], lesion -> lesion.getUltimoTratamiento() == null
                ? "" : lesion.getUltimoTratamiento().getEstado().toString()));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[6], RegistroLesionController::fin));
        Tablas.distribuirAnchos(tablaHistorial, 90, 117, 75, 85, 190, 80, 90);
        Tablas.ajustarAltura(tablaHistorial, 1, 4, Tablas.ALTO_FILA_COMPACTA);
        tablaHistorial.setPlaceholder(new Label("Elija un jugador para ver su historial."));

        Campos.mostrarCon(comboJugador, Campos::describirJugador);
        Campos.mostrarCon(comboTipo, TipoLesion::getNombre);
        Campos.mostrarCon(comboZona, ZonaCorporal::getNombre);
        Campos.mostrarCon(comboGravedad, Gravedad::getEtiqueta);
        comboGravedad.getItems().setAll(Gravedad.values());
        Campos.configurarFecha(campoFecha, hoy());
        comboJugador.valueProperty().addListener((propiedad, anterior, jugador) -> cargarHistorial(jugador));
        botonRegistrar.disableProperty().bind(ocupadoProperty());
        etiquetaResultado.managedProperty().bind(etiquetaResultado.visibleProperty());
        limpiar();
    }

    @Override
    public String getTitulo() {
        return "Registrar lesión";
    }

    @Override
    public void alMostrar() {
        LesionService lesiones = servicios().lesiones();
        ejecutar(() -> new Catalogos(servicios().jugadores().listar(false), lesiones.listarTipos(),
                lesiones.listarZonas()), catalogos -> {
                    comboJugador.getItems().setAll(catalogos.jugadores());
                    comboTipo.getItems().setAll(catalogos.tipos());
                    comboZona.getItems().setAll(catalogos.zonas());
                });
    }


    /** Historial automático del jugador elegido (CU-12). */
    private void cargarHistorial(Jugador jugador) {
        if (jugador == null) {
            tablaHistorial.getItems().clear();
            return;
        }
        int id = jugador.getId();
        ejecutar(() -> servicios().lesiones().consultarHistorial(id), historial -> {
            tablaHistorial.setPlaceholder(new Label("Sin lesiones registradas."));
            tablaHistorial.getItems().setAll(historial);
            if (!historial.isEmpty()) {
                tablaHistorial.scrollTo(historial.size() - 1);
            }
        });
    }

    /**
     * Valida la lesión y verifica la recurrencia; si hay antecedente, muestra
     * la alerta (informativa) y después registra.
     */
    @FXML
    private void registrar() {
        List<String> errores = new ArrayList<>();
        LocalDate fecha = Campos.leerFecha(campoFecha, "fecha de la lesión", errores);
        if (!errores.isEmpty()) {
            mostrarErrores(errores);
            return;
        }
        String descripcion = campoDescripcion.getText() == null ? "" : campoDescripcion.getText().trim();
        Lesion nueva = new Lesion(comboJugador.getValue(), comboTipo.getValue(), comboZona.getValue(),
                comboGravedad.getValue(), fecha, Texto.estaVacio(descripcion) ? null : descripcion);
        etiquetaResultado.setVisible(false);
        LesionService lesiones = servicios().lesiones();
        ejecutar(() -> {
            lesiones.validar(nueva);
            return lesiones.verificarRecurrencia(nueva, lesiones.buscarAntecedentes(nueva));
        }, antecedente -> {
            antecedente.ifPresent(lesion -> presentar(AlertaRecurrencia.crear(ventana(), lesion, nueva.getFecha())));
            ejecutar(() -> lesiones.registrarLesion(nueva), id -> registrada(nueva, id));
        });
    }

    private void registrada(Lesion nueva, int id) {
        etiquetaResultado.setText("Lesión registrada con el N° " + id + ".");
        etiquetaResultado.setVisible(true);
        Jugador jugador = nueva.getJugador();
        limpiar();
        cargarHistorial(jugador);
        if (confirmar("Lesión N° " + id + " registrada para " + jugador.getNombreCompleto() + ".\n\n"
                + "¿Registrar el tratamiento ahora?")) {
            ControladorPantalla pantalla = contexto().abrirOpcion("tratamientos.fxml");
            if (pantalla instanceof TratamientosController tratamientos) {
                tratamientos.preseleccionar(jugador.getId(), id);
            }
        }
    }

    /** Deja los datos de la lesión vacíos (conserva el jugador y su historial). */
    @FXML
    private void limpiar() {
        comboTipo.setValue(null);
        comboZona.setValue(null);
        comboGravedad.setValue(null);
        Campos.mostrarFecha(campoFecha, hoy());
        campoDescripcion.clear();
    }

    private static String fin(Lesion lesion) {
        Tratamiento ultimo = lesion.getUltimoTratamiento();
        return ultimo == null || ultimo.getFechaFin() == null ? "" : Fechas.formatear(ultimo.getFechaFin());
    }
}

