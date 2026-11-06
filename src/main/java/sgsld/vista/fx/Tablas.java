package sgsld.vista.fx;

import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;

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

    /** Alto de cada fila, en píxeles (como en la Figura 12). */
    static final double ALTO_FILA = 44;
    /** Alto de fila de las tablas de administración, más compactas. */
    static final double ALTO_FILA_COMPACTA = 34;
    /** Alto del encabezado (coincide con {@code sgsld.css}). */
    static final double ALTO_ENCABEZADO = 42;

    private Tablas() {
    }

    /**
     * Reparte el ancho de la tabla entre sus columnas según los pesos (por
     * ejemplo, anchos de referencia en píxeles). Descuenta la barra vertical
     * solo mientras está visible, de modo que nunca aparece la horizontal.
     *
     * @param tabla tabla con las columnas ya agregadas
     * @param pesos un peso por columna
     */
    static void distribuirAnchos(TableView<?> tabla, double... pesos) {
        DoubleProperty anchoBarra = new SimpleDoubleProperty(0);
        if (tabla.getSkin() != null) {
            seguirBarraVertical(tabla, anchoBarra);
        }
        tabla.skinProperty().addListener((propiedad, anterior, piel) -> seguirBarraVertical(tabla, anchoBarra));
        double total = 0;
        for (double peso : pesos) {
            total += peso;
        }
        for (int i = 0; i < pesos.length; i++) {
            TableColumn<?, ?> columna = tabla.getColumns().get(i);
            double fraccion = pesos[i] / total;
            columna.setResizable(false);
            columna.prefWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.floor((tabla.getWidth() - 4 - anchoBarra.get()) * fraccion),
                    tabla.widthProperty(), anchoBarra));
        }
    }

    /** Vincula el ancho reservado con la barra vertical de la tabla (existe una vez creada la piel). */
    private static void seguirBarraVertical(TableView<?> tabla, DoubleProperty anchoBarra) {
        for (Node nodo : tabla.lookupAll(".scroll-bar")) {
            if (nodo instanceof ScrollBar barra && barra.getOrientation() == Orientation.VERTICAL) {
                anchoBarra.bind(Bindings.when(barra.visibleProperty()).then(barra.widthProperty()).otherwise(0.0));
                return;
            }
        }
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
        // El texto completo aparece al pasar el mouse, por si la columna lo corta.
        columna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String texto, boolean vacia) {
                super.updateItem(texto, vacia);
                setText(vacia ? null : texto);
                setTooltip(vacia || texto == null || texto.isEmpty() ? null : new Tooltip(texto));
            }
        });
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
        ajustarAltura(tabla, filasMinimas, Integer.MAX_VALUE);
    }

    /**
     * Ajusta el alto de la tabla a sus filas, entre un mínimo y un máximo; con
     * más filas que el máximo aparece la barra de desplazamiento de la tabla.
     *
     * @param tabla        tabla
     * @param filasMinimas filas visibles como mínimo
     * @param filasMaximas filas visibles como máximo
     */
    static void ajustarAltura(TableView<?> tabla, int filasMinimas, int filasMaximas) {
        ajustarAltura(tabla, filasMinimas, filasMaximas, ALTO_FILA);
    }

    /**
     * Ajusta el alto de la tabla a sus filas, con el alto de fila indicado.
     *
     * @param tabla        tabla
     * @param filasMinimas filas visibles como mínimo
     * @param filasMaximas filas visibles como máximo
     * @param altoFila     alto de cada fila, en píxeles
     */
    static void ajustarAltura(TableView<?> tabla, int filasMinimas, int filasMaximas, double altoFila) {
        ajustarAltura(tabla, filasMinimas, filasMaximas, altoFila, ALTO_ENCABEZADO);
    }

    /**
     * Ajusta el alto de la tabla a sus filas, con el alto de fila y de
     * encabezado indicados (por ejemplo, encabezados de dos renglones).
     *
     * @param tabla          tabla
     * @param filasMinimas   filas visibles como mínimo
     * @param filasMaximas   filas visibles como máximo
     * @param altoFila       alto de cada fila, en píxeles
     * @param altoEncabezado alto del encabezado, en píxeles
     */
    static void ajustarAltura(TableView<?> tabla, int filasMinimas, int filasMaximas, double altoFila,
                              double altoEncabezado) {
        tabla.setFixedCellSize(altoFila);
        DoubleBinding alto = Bindings.createDoubleBinding(() -> altoEncabezado + altoFila
                        * Math.min(filasMaximas, Math.max(filasMinimas, tabla.getItems().size())) + 4,
                tabla.getItems());
        tabla.prefHeightProperty().bind(alto);
        tabla.minHeightProperty().bind(alto);
        tabla.maxHeightProperty().bind(alto);
    }
}
