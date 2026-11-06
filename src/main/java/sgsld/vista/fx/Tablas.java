package sgsld.vista.fx;

import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import sgsld.modelo.EstadoDisponibilidad;

/**
 * Ayudas para las tablas de la interfaz: columnas de solo lectura, altura
 * ajustada a la cantidad de filas (como en los prototipos) y el "chip" del
 * estado de disponibilidad.
 * <p>
 * Las columnas no se ordenan con un clic: el orden lo definen los servicios
 * con {@code Algoritmos.ordenarPorInsercion}.
 */
final class Tablas {

    /** Alto de cada fila, en píxeles. */
    static final double ALTO_FILA = 44;
    /** Alto del encabezado (coincide con {@code sgsld.css}). */
    static final double ALTO_ENCABEZADO = 42;

    private Tablas() {
    }

    /**
     * Columna de texto.
     *
     * @param titulo encabezado
     * @param valor  cómo obtener el texto de cada fila
     * @param <S>    tipo de las filas
     * @return la columna
     */
    static <S> TableColumn<S, String> columna(String titulo, Function<S, String> valor) {
        TableColumn<S, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(valor.apply(celda.getValue())));
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    /**
     * Columna numérica, alineada a la derecha.
     *
     * @param titulo encabezado
     * @param valor  cómo obtener el texto de cada fila
     * @param <S>    tipo de las filas
     * @return la columna
     */
    static <S> TableColumn<S, String> columnaNumerica(String titulo, Function<S, String> valor) {
        TableColumn<S, String> columna = columna(titulo, valor);
        columna.getStyleClass().add("columna-numero");
        return columna;
    }

    /**
     * Columna que muestra el estado de disponibilidad como chip de color.
     *
     * @param titulo encabezado
     * @param valor  cómo obtener el estado de cada fila
     * @param <S>    tipo de las filas
     * @return la columna
     */
    static <S> TableColumn<S, EstadoDisponibilidad> columnaEstado(String titulo,
                                                                 Function<S, EstadoDisponibilidad> valor) {
        TableColumn<S, EstadoDisponibilidad> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(valor.apply(celda.getValue())));
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(EstadoDisponibilidad estado, boolean vacia) {
                super.updateItem(estado, vacia);
                setText(null);
                setGraphic(vacia || estado == null ? null : chip(estado));
            }
        });
        columna.setSortable(false);
        columna.setReorderable(false);
        return columna;
    }

    /**
     * Etiqueta redondeada con el color del estado: naranja "En tratamiento",
     * verde "Disponible".
     *
     * @param estado estado de disponibilidad
     * @return el chip
     */
    static Label chip(EstadoDisponibilidad estado) {
        Label chip = new Label(estado.toString());
        chip.getStyleClass().addAll("chip",
                estado == EstadoDisponibilidad.EN_TRATAMIENTO ? "chip-en-tratamiento" : "chip-disponible");
        return chip;
    }

    /**
     * Ajusta el alto de la tabla a sus filas, de modo que no quede espacio
     * vacío debajo (con un mínimo de filas para el texto "sin resultados").
     *
     * @param tabla        tabla
     * @param filasMinimas filas visibles como mínimo
     */
    static void ajustarAltura(TableView<?> tabla, int filasMinimas) {
        tabla.setFixedCellSize(ALTO_FILA);
        DoubleBinding alto = Bindings.createDoubleBinding(
                () -> ALTO_ENCABEZADO + ALTO_FILA * Math.max(filasMinimas, tabla.getItems().size()) + 2,
                tabla.getItems());
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);
        tabla.maxHeightProperty().bind(alto);
    }
}
