package sgsld.vista.fx;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import sgsld.modelo.dto.EstadoJugadorDTO;
import sgsld.util.Texto;

/**
 * Estado actual de disponibilidad (CU-08, Figura 12, {@code estado_actual.fxml}).
 * Vista de solo lectura para el Cuerpo Técnico: jugador, categoría y estado,
 * con filtro por categoría y búsqueda por apellido o nombre (sin tildes ni
 * mayúsculas). No muestra datos clínicos.
 */
public class EstadoActualController extends ControladorPantalla {

    private static final String TODAS = "Todas";
    /** Encabezados de la tabla (cantidad fija). */
    private static final String[] ENCABEZADOS = {"Jugador", "Categoría", "Estado"};

    @FXML
    private ComboBox<String> comboCategoria;
    @FXML
    private TextField campoBusqueda;
    @FXML
    private TableView<EstadoJugadorDTO> tabla;

    private List<EstadoJugadorDTO> filas = new ArrayList<>();

    @FXML
    private void initialize() {
        TableColumn<EstadoJugadorDTO, String> jugador = Tablas.columna(ENCABEZADOS[0], EstadoJugadorDTO::jugador);
        TableColumn<EstadoJugadorDTO, String> categoria = Tablas.columna(ENCABEZADOS[1], EstadoJugadorDTO::categoria);
        TableColumn<EstadoJugadorDTO, ?> estado = Tablas.columnaEstado(ENCABEZADOS[2], EstadoJugadorDTO::estado);
        tabla.getColumns().add(jugador);
        tabla.getColumns().add(categoria);
        tabla.getColumns().add(estado);
        Tablas.distribuirAnchos(tabla, 490, 365, 605);
        tabla.setPlaceholder(new Label("No hay jugadores para los filtros seleccionados."));
        Tablas.ajustarAltura(tabla, 1);
        comboCategoria.getItems().add(TODAS);
        comboCategoria.getSelectionModel().select(TODAS);
        comboCategoria.setOnAction(evento -> filtrar());
        campoBusqueda.textProperty().addListener((propiedad, anterior, nuevo) -> filtrar());
    }

    @Override
    public String getTitulo() {
        return "Estado actual de disponibilidad";
    }

    @Override
    public void alMostrar() {
        ejecutar(() -> servicios().consultas().consultarEstadoActual(), this::mostrar);
    }

    /** Guarda las filas (ya ordenadas por categoría y jugador) y arma el filtro de categorías. */
    private void mostrar(List<EstadoJugadorDTO> leidas) {
        filas = leidas;
        List<String> categorias = new ArrayList<>();
        categorias.add(TODAS);
        for (EstadoJugadorDTO fila : filas) {
            if (!categorias.contains(fila.categoria())) {
                categorias.add(fila.categoria());
            }
        }
        comboCategoria.getItems().setAll(categorias);
        comboCategoria.getSelectionModel().select(TODAS);
        filtrar();
    }

    private void filtrar() {
        String categoria = comboCategoria.getValue();
        String busqueda = campoBusqueda.getText();
        List<EstadoJugadorDTO> visibles = new ArrayList<>();
        for (EstadoJugadorDTO fila : filas) {
            boolean deLaCategoria = categoria == null || TODAS.equals(categoria) || categoria.equals(fila.categoria());
            if (deLaCategoria && (Texto.estaVacio(busqueda) || Texto.contiene(fila.jugador(), busqueda.trim()))) {
                visibles.add(fila);
            }
        }
        tabla.getItems().setAll(visibles);
    }
}
