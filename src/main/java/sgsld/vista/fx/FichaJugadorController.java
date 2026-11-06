package sgsld.vista.fx;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import sgsld.controlador.Formatos;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Jugador;
import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.dto.FichaJugadorDTO;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.Fechas;
import sgsld.util.Texto;

/**
 * Consultar ficha (CU-10, {@code ficha_jugador.fxml}). Roles: Coordinador y Kinesiólogo.
 * <p>
 * Búsqueda por DNI exacto (búsqueda binaria en el servicio) o por parte del
 * apellido (secuencial); muestra los datos del jugador, su estado (chip) y su
 * historial (CU-12). Al elegir una lesión se ven sus tratamientos y, al
 * elegir un tratamiento, sus evoluciones. Incluye a los dados de baja, cuyo
 * historial se conserva.
 */
public class FichaJugadorController extends ControladorPantalla {

    private static final String[] HISTORIAL = {"N°", "Fecha", "Zona corporal", "Tipo", "Gravedad", "Descripción",
        "Estado"};
    private static final String[] TRATAMIENTOS = {"N°", "Tipo", "Responsable", "Inicio", "Fin", "Estado", "Duración"};
    private static final String[] EVOLUCIONES = {"Fecha", "Resultado", "Observación"};

    @FXML
    private ComboBox<Jugador> comboJugador;
    @FXML
    private TextField campoBusqueda;
    @FXML
    private VBox panelFicha;
    @FXML
    private Label etiquetaNombre;
    @FXML
    private Label datoDni;
    @FXML
    private Label datoNacimiento;
    @FXML
    private Label datoCategoria;
    @FXML
    private HBox datoEstado;
    @FXML
    private Label datoPosicion;
    @FXML
    private Label datoCamiseta;
    @FXML
    private Label datoSituacion;
    @FXML
    private TableView<Lesion> tablaHistorial;
    @FXML
    private Label tituloTratamientos;
    @FXML
    private TableView<Tratamiento> tablaTratamientos;
    @FXML
    private Label tituloEvoluciones;
    @FXML
    private TableView<Evolucion> tablaEvoluciones;

    private List<Jugador> todos = new ArrayList<>();

    @FXML
    private void initialize() {
        tablaHistorial.getColumns().add(Tablas.columnaNumerica(HISTORIAL[0], l -> String.valueOf(l.getId())));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[1], l -> Fechas.formatear(l.getFecha())));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[2], l -> l.getZona().getNombre()));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[3], l -> l.getTipo().getNombre()));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[4], l -> l.getGravedad().getEtiqueta()));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[5],
                l -> l.getDescripcion() == null ? "" : l.getDescripcion()));
        tablaHistorial.getColumns().add(Tablas.columna(HISTORIAL[6], Formatos::estadoDeLesion));
        Tablas.distribuirAnchos(tablaHistorial, 45, 105, 120, 95, 95, 175, 175);
        Tablas.ajustarAltura(tablaHistorial, 1, 4, Tablas.ALTO_FILA_COMPACTA);
        tablaHistorial.setPlaceholder(new Label("Sin lesiones registradas."));
        tablaHistorial.getSelectionModel().selectedItemProperty().addListener(
                (propiedad, anterior, lesion) -> mostrarTratamientos(lesion));

        tablaTratamientos.getColumns().add(Tablas.columnaNumerica(TRATAMIENTOS[0], t -> String.valueOf(t.getId())));
        tablaTratamientos.getColumns().add(Tablas.columna(TRATAMIENTOS[1], Tratamiento::getTipo));
        tablaTratamientos.getColumns().add(Tablas.columna(TRATAMIENTOS[2], t -> t.getKinesiologo().getNombreCompleto()));
        tablaTratamientos.getColumns().add(Tablas.columna(TRATAMIENTOS[3], t -> Fechas.formatear(t.getFechaInicio())));
        tablaTratamientos.getColumns().add(Tablas.columna(TRATAMIENTOS[4],
                t -> t.getFechaFin() == null ? "" : Fechas.formatear(t.getFechaFin())));
        tablaTratamientos.getColumns().add(Tablas.columna(TRATAMIENTOS[5], t -> t.getEstado().toString()));
        tablaTratamientos.getColumns().add(Tablas.columna(TRATAMIENTOS[6], this::duracion));
        Tablas.distribuirAnchos(tablaTratamientos, 42, 190, 145, 102, 102, 90, 160);
        Tablas.ajustarAltura(tablaTratamientos, 1, 3, Tablas.ALTO_FILA_COMPACTA);
        tablaTratamientos.setPlaceholder(new Label("La lesión no tiene tratamientos registrados."));
        tablaTratamientos.getSelectionModel().selectedItemProperty().addListener(
                (propiedad, anterior, tratamiento) -> mostrarEvoluciones(tratamiento));

        tablaEvoluciones.getColumns().add(Tablas.columna(EVOLUCIONES[0], e -> Fechas.formatear(e.getFecha())));
        tablaEvoluciones.getColumns().add(Tablas.columna(EVOLUCIONES[1], e -> e.getResultado().getEtiqueta()));
        tablaEvoluciones.getColumns().add(Tablas.columna(EVOLUCIONES[2], Evolucion::getObservacion));
        Tablas.distribuirAnchos(tablaEvoluciones, 100, 110, 580);
        Tablas.ajustarAltura(tablaEvoluciones, 1, 4, Tablas.ALTO_FILA_COMPACTA);
        tablaEvoluciones.setPlaceholder(new Label("Sin evoluciones registradas."));

        Campos.mostrarCon(comboJugador, jugador -> Campos.describirJugador(jugador)
                + (jugador.isActivo() ? "" : " (dado de baja)"));
        comboJugador.valueProperty().addListener((propiedad, anterior, jugador) -> cargarFicha(jugador));
        panelFicha.managedProperty().bind(panelFicha.visibleProperty());
        panelFicha.setVisible(false);
    }

    @Override
    public String getTitulo() {
        return "Ficha del jugador";
    }

    @Override
    public void alMostrar() {
        ejecutar(() -> servicios().jugadores().listar(true), jugadores -> {
            todos = jugadores;
            comboJugador.getItems().setAll(jugadores);
        });
    }

    /**
     * Busca por DNI si el texto son solo dígitos o por parte del apellido; el
     * combo queda con las coincidencias y, si hay una sola, se muestra su ficha.
     */
    @FXML
    private void buscar() {
        String texto = Campos.leerTexto(campoBusqueda);
        if (texto.isEmpty()) {
            comboJugador.getItems().setAll(todos);
            comboJugador.setPromptText("Elegir jugador…");
            return;
        }
        if (texto.chars().allMatch(Character::isDigit)) {
            ejecutar(() -> List.of(servicios().jugadores().buscarPorDni(texto)), this::mostrarCoincidencias, error -> {
                if (error instanceof EntidadNoEncontradaException) {
                    mostrarCoincidencias(List.of());
                } else {
                    informarError(error);
                }
            });
        } else {
            ejecutar(() -> servicios().jugadores().buscarPorApellido(texto), this::mostrarCoincidencias);
        }
    }

    private void mostrarCoincidencias(List<Jugador> encontrados) {
        comboJugador.setValue(null);
        comboJugador.getItems().setAll(encontrados);
        comboJugador.setPromptText(encontrados.isEmpty() ? "Sin coincidencias"
                : encontrados.size() + (encontrados.size() == 1 ? " coincidencia" : " coincidencias") + ": elija…");
        if (encontrados.size() == 1) {
            comboJugador.setValue(encontrados.get(0));
        } else if (!encontrados.isEmpty() && comboJugador.getScene() != null && comboJugador.getScene().getWindow() != null
                && comboJugador.getScene().getWindow().isShowing()) {
            comboJugador.show();
        }
    }

    private void cargarFicha(Jugador jugador) {
        if (jugador == null) {
            panelFicha.setVisible(false);
            return;
        }
        int id = jugador.getId();
        ejecutar(() -> servicios().consultas().consultarFicha(id), this::mostrarFicha);
    }

    private void mostrarFicha(FichaJugadorDTO ficha) {
        Jugador jugador = ficha.jugador();
        etiquetaNombre.setText(jugador.getNombreCompleto());
        datoDni.setText(jugador.getDni());
        datoNacimiento.setText(Fechas.formatear(jugador.getFechaNacimiento()) + " (" + jugador.getEdad(hoy())
                + " años)");
        datoCategoria.setText(jugador.getCategoria().getNombre() + " (" + jugador.getCategoria().getRangoEtario() + ")");
        datoEstado.getChildren().setAll(Tablas.chip(ficha.estado()));
        datoPosicion.setText(jugador.getPosicion());
        datoCamiseta.setText(jugador.getNumeroCamiseta() == null ? "—" : String.valueOf(jugador.getNumeroCamiseta()));
        datoSituacion.setText(jugador.isActivo() ? "Activo" : "Dado de baja (se conserva su historial)");
        tablaHistorial.getItems().setAll(ficha.historial());
        panelFicha.setVisible(true);
        if (ficha.historial().isEmpty()) {
            mostrarTratamientos(null);
        } else {
            // La lesión más reciente queda elegida: se ven sus tratamientos y evoluciones.
            tablaHistorial.getSelectionModel().select(ficha.historial().size() - 1);
            tablaHistorial.scrollTo(ficha.historial().size() - 1);
        }
    }

    private void mostrarTratamientos(Lesion lesion) {
        if (lesion == null) {
            tituloTratamientos.setText("TRATAMIENTOS");
            tablaTratamientos.getItems().clear();
            mostrarEvoluciones(null);
            return;
        }
        tituloTratamientos.setText(("Tratamientos de la lesión N° " + lesion.getId() + " · "
                + Formatos.lesion(lesion)).toUpperCase(Texto.LOCALE));
        tablaTratamientos.getItems().setAll(lesion.getTratamientos());
        if (lesion.getTratamientos().isEmpty()) {
            mostrarEvoluciones(null);
        } else {
            tablaTratamientos.getSelectionModel().select(lesion.getTratamientos().size() - 1);
        }
    }

    private void mostrarEvoluciones(Tratamiento tratamiento) {
        if (tratamiento == null) {
            tituloEvoluciones.setText("EVOLUCIONES");
            tablaEvoluciones.getItems().clear();
            return;
        }
        tituloEvoluciones.setText("EVOLUCIONES DEL TRATAMIENTO N° " + tratamiento.getId());
        tablaEvoluciones.getItems().setAll(tratamiento.getEvoluciones());
    }

    private String duracion(Tratamiento tratamiento) {
        return tratamiento.estaEnCurso()
                ? Formatos.dias(tratamiento.getDiasTranscurridos(hoy())) + " (en curso)"
                : Formatos.dias(tratamiento.getDuracionDias());
    }
}
