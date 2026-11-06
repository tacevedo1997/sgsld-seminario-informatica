package sgsld.vista.fx;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import sgsld.controlador.Formatos;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;
import sgsld.util.Fechas;

/**
 * Registrar tratamiento (CU-05, {@code tratamientos.fxml}). Rol: Kinesiólogo.
 * <p>
 * Se elige el jugador, luego una de sus lesiones (con su estado) y se
 * completan el responsable (se propone el kinesiólogo de la sesión), la fecha
 * de inicio, el tipo y las observaciones. El tratamiento queda "En curso"; si
 * la lesión ya tiene uno en curso, el servicio lo rechaza.
 */
public class TratamientosController extends ControladorPantalla {

    /** Encabezados de la tabla de lesiones (cantidad fija). */
    private static final String[] LESIONES = {"N°", "Fecha", "Zona corporal", "Tipo", "Gravedad", "Estado"};

    @FXML
    private ComboBox<Jugador> comboJugador;
    @FXML
    private TableView<Lesion> tablaLesiones;
    @FXML
    private Label tituloDatos;
    @FXML
    private ComboBox<Kinesiologo> comboResponsable;
    @FXML
    private DatePicker campoInicio;
    @FXML
    private TextField campoTipo;
    @FXML
    private TextArea campoObservaciones;
    @FXML
    private Button botonRegistrar;
    @FXML
    private Label etiquetaResultado;

    private Kinesiologo propio;
    private Integer jugadorPendiente;
    private Integer lesionPendiente;
    /** Lesión a seleccionar cuando el combo de jugador cambia por código. */
    private Integer lesionASeleccionar;

    /** Jugadores, kinesiólogos y el kinesiólogo de la sesión, leídos en una misma tarea. */
    private record Datos(List<Jugador> jugadores, List<Kinesiologo> kinesiologos, Optional<Kinesiologo> propio) {
    }

    @FXML
    private void initialize() {
        tablaLesiones.getColumns().add(Tablas.columnaNumerica(LESIONES[0], lesion -> String.valueOf(lesion.getId())));
        tablaLesiones.getColumns().add(Tablas.columna(LESIONES[1], lesion -> Fechas.formatear(lesion.getFecha())));
        tablaLesiones.getColumns().add(Tablas.columna(LESIONES[2], lesion -> lesion.getZona().getNombre()));
        tablaLesiones.getColumns().add(Tablas.columna(LESIONES[3], lesion -> lesion.getTipo().getNombre()));
        tablaLesiones.getColumns().add(Tablas.columna(LESIONES[4], lesion -> lesion.getGravedad().getEtiqueta()));
        tablaLesiones.getColumns().add(Tablas.columna(LESIONES[5], Formatos::estadoDeLesion));
        Tablas.distribuirAnchos(tablaLesiones, 50, 100, 140, 110, 100, 200);
        Tablas.ajustarAltura(tablaLesiones, 1, 4, Tablas.ALTO_FILA_COMPACTA);
        tablaLesiones.setPlaceholder(new Label("Elija un jugador para ver sus lesiones."));
        tablaLesiones.getSelectionModel().selectedItemProperty().addListener(
                (propiedad, anterior, lesion) -> actualizarTitulo(lesion));

        Campos.mostrarCon(comboJugador, Campos::describirJugador);
        Campos.mostrarCon(comboResponsable, kinesiologo -> kinesiologo.getNombreCompleto() + " ("
                + kinesiologo.getMatricula() + ")");
        Campos.configurarFecha(campoInicio, hoy());
        comboJugador.valueProperty().addListener(
                (propiedad, anterior, jugador) -> cargarLesiones(jugador, lesionASeleccionar));
        botonRegistrar.disableProperty().bind(ocupadoProperty());
        etiquetaResultado.managedProperty().bind(etiquetaResultado.visibleProperty());
        actualizarTitulo(null);
        limpiar();
    }

    @Override
    public String getTitulo() {
        return "Tratamientos";
    }

    @Override
    public void alMostrar() {
        int idUsuario = usuario().getId();
        ejecutar(() -> new Datos(servicios().jugadores().listar(false), servicios().kinesiologos().listar(),
                servicios().kinesiologos().buscarPorUsuario(idUsuario)), datos -> {
                    comboJugador.getItems().setAll(datos.jugadores());
                    comboResponsable.getItems().setAll(datos.kinesiologos());
                    propio = datos.propio().orElse(null);
                    proponerResponsable();
                    aplicarPreseleccion();
                });
    }

    /**
     * Deja elegidos el jugador y la lesión (por ejemplo, la que se acaba de
     * registrar en "Registrar lesión").
     *
     * @param idJugador número del jugador
     * @param idLesion  número de la lesión
     */
    void preseleccionar(int idJugador, int idLesion) {
        jugadorPendiente = idJugador;
        lesionPendiente = idLesion;
        if (!comboJugador.getItems().isEmpty()) {
            aplicarPreseleccion();
        }
    }

    private void aplicarPreseleccion() {
        if (jugadorPendiente != null) {
            int idJugador = jugadorPendiente;
            Integer idLesion = lesionPendiente;
            jugadorPendiente = null;
            lesionPendiente = null;
            elegirJugador(idJugador, idLesion);
        }
    }

    /** Elige el jugador y carga sus lesiones, seleccionando la indicada (si se indica). */
    private void elegirJugador(int idJugador, Integer idLesion) {
        Jugador actual = comboJugador.getValue();
        if (actual != null && actual.getId() == idJugador) {
            cargarLesiones(actual, idLesion);
        } else {
            lesionASeleccionar = idLesion;
            Campos.elegirPorId(comboJugador, idJugador);
            lesionASeleccionar = null;
        }
    }

    private void cargarLesiones(Jugador jugador, Integer idLesion) {
        if (jugador == null) {
            tablaLesiones.getItems().clear();
            return;
        }
        int id = jugador.getId();
        ejecutar(() -> servicios().lesiones().consultarHistorial(id), historial -> {
            tablaLesiones.setPlaceholder(new Label("El jugador no tiene lesiones registradas. "
                    + "Registre primero la lesión."));
            tablaLesiones.getItems().setAll(historial);
            for (Lesion lesion : historial) {
                if (idLesion != null && lesion.getId() == idLesion) {
                    tablaLesiones.getSelectionModel().select(lesion);
                    tablaLesiones.scrollTo(lesion);
                }
            }
        });
    }

    private void actualizarTitulo(Lesion lesion) {
        tituloDatos.setText(lesion == null ? "DATOS DEL TRATAMIENTO" : "DATOS DEL TRATAMIENTO · LESIÓN N° " + lesion.getId());
    }

    private void proponerResponsable() {
        comboResponsable.setValue(null);
        if (propio != null) {
            Campos.elegirPorId(comboResponsable, propio.getId());
        }
    }

    /** Registra el tratamiento; el servicio valida todo junto (lesión, responsable, fechas, tipo). */
    @FXML
    private void registrar() {
        List<String> errores = new ArrayList<>();
        Lesion lesion = tablaLesiones.getSelectionModel().getSelectedItem();
        if (lesion == null) {
            errores.add("Debe seleccionar la lesión a tratar en la tabla.");
        }
        LocalDate inicio = Campos.leerFecha(campoInicio, "fecha de inicio", errores);
        if (!errores.isEmpty()) {
            mostrarErrores(errores);
            return;
        }
        String observaciones = campoObservaciones.getText() == null ? "" : campoObservaciones.getText().trim();
        Tratamiento nuevo = new Tratamiento(lesion, comboResponsable.getValue(), inicio, Campos.leerTexto(campoTipo),
                observaciones.isEmpty() ? null : observaciones);
        int idJugador = lesion.getJugador().getId();
        int idLesion = lesion.getId();
        etiquetaResultado.setVisible(false);
        ejecutar(() -> servicios().tratamientos().registrarTratamiento(nuevo), id -> {
            etiquetaResultado.setText("Tratamiento N° " + id + " registrado (En curso).");
            etiquetaResultado.setVisible(true);
            limpiar();
            elegirJugador(idJugador, idLesion);
        });
    }

    /** Vuelve a los valores propuestos (responsable de la sesión, inicio hoy). */
    @FXML
    private void limpiar() {
        proponerResponsable();
        Campos.mostrarFecha(campoInicio, hoy());
        campoTipo.clear();
        campoObservaciones.clear();
    }
}
