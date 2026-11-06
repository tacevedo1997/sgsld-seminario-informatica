package sgsld.vista.fx;

import java.util.List;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.text.Text;
import javafx.util.StringConverter;

import sgsld.util.Texto;

/**
 * Gráfico de barras horizontal de los reportes (Figura 13): barras verdes,
 * el valor al final de cada barra, sin leyenda ni animación y las categorías
 * en el mismo orden que la tabla (de arriba hacia abajo). Cada reporte usa un
 * gráfico nuevo, de modo que no quedan categorías ni valores del anterior.
 */
final class GraficoBarras extends BarChart<Number, String> {

    /** Alto que ocupa cada categoría y grosor de cada barra, en píxeles. */
    private static final double ALTO_CATEGORIA = 44;
    private static final double GROSOR_BARRA = 26;
    /** Alto del eje de valores y los márgenes del gráfico. */
    private static final double ALTO_FIJO = 60;

    private final NumberAxis ejeValores;
    private final CategoryAxis ejeCategorias;

    GraficoBarras() {
        super(new NumberAxis(), new CategoryAxis());
        ejeValores = (NumberAxis) getXAxis();
        ejeCategorias = (CategoryAxis) getYAxis();
        ejeValores.setAutoRanging(false);
        ejeValores.setMinorTickVisible(false);
        ejeValores.setTickMarkVisible(false);
        ejeCategorias.setTickMarkVisible(false);
        setAnimated(false);
        setLegendVisible(false);
        setHorizontalGridLinesVisible(false);
        setVerticalGridLinesVisible(false);
        setVerticalZeroLineVisible(false);
        getStyleClass().add("grafico-reporte");
    }

    /**
     * Muestra una barra por categoría.
     *
     * @param categorias etiquetas, en el orden de la tabla
     * @param valores    valor de cada barra
     * @param textos     texto que se muestra al final de cada barra
     * @param enteros    {@code true} si los valores son cantidades (escala de enteros)
     */
    void mostrar(List<String> categorias, List<Double> valores, List<String> textos, boolean enteros) {
        double maximo = 0;
        for (double valor : valores) {
            maximo = Math.max(maximo, valor);
        }
        double unidad = unidad(maximo, enteros);
        ejeValores.setLowerBound(0);
        ejeValores.setTickUnit(unidad);
        ejeValores.setTickLabelFormatter(new StringConverter<>() {
            /** Solo las divisiones (0, 1, 2...): no el límite superior, que deja lugar al número de la barra. */
            @Override
            public String toString(Number valor) {
                double divisiones = valor.doubleValue() / unidad;
                return Math.abs(divisiones - Math.rint(divisiones)) < 1e-9
                        ? String.format(Texto.LOCALE, "%,.0f", valor.doubleValue()) : "";
            }

            @Override
            public Number fromString(String texto) {
                return Double.valueOf(texto);
            }
        });
        // Un poco más que el último valor, para que entre el número al final de la barra.
        ejeValores.setUpperBound(Math.max(unidad, Math.ceil(maximo / unidad) * unidad) + unidad * 0.45);

        // El eje de categorías dibuja la primera abajo: se agregan de la última a la primera.
        XYChart.Series<Number, String> serie = new XYChart.Series<>();
        for (int i = categorias.size() - 1; i >= 0; i--) {
            Text texto = new Text(textos.get(i));
            texto.getStyleClass().add("grafico-valor");
            XYChart.Data<Number, String> dato = new XYChart.Data<>(valores.get(i), categorias.get(i));
            dato.setExtraValue(texto);
            serie.getData().add(dato);
        }
        int cantidad = Math.max(1, categorias.size());
        setCategoryGap(Math.max(10, ALTO_CATEGORIA - GROSOR_BARRA));
        setPrefHeight(Math.max(220, ALTO_FIJO + ALTO_CATEGORIA * cantidad));
        setMinHeight(getPrefHeight());
        getData().setAll(List.of(serie));
    }

    /** Divisiones del eje: de a 1 en las cantidades chicas; si no, un múltiplo "redondo". */
    private static double unidad(double maximo, boolean enteros) {
        if (enteros && maximo <= 10) {
            return 1;
        }
        double bruta = Math.max(maximo, 1) / 5;
        double potencia = Math.pow(10, Math.floor(Math.log10(bruta)));
        for (double paso : new double[] {1, 2, 5, 10}) {
            if (paso * potencia >= bruta) {
                return paso * potencia;
            }
        }
        return 10 * potencia;
    }

    /** Después de ubicar las barras, pone cada valor a la derecha de su barra. */
    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();
        for (XYChart.Series<Number, String> serie : getData()) {
            for (XYChart.Data<Number, String> dato : serie.getData()) {
                Node barra = dato.getNode();
                if (barra == null || !(dato.getExtraValue() instanceof Text texto)) {
                    continue;
                }
                if (!getPlotChildren().contains(texto)) {
                    getPlotChildren().add(texto);
                }
                Bounds limites = barra.getBoundsInParent();
                Bounds tamano = texto.getLayoutBounds();
                texto.setLayoutX(limites.getMaxX() + 8);
                texto.setLayoutY(limites.getMinY() + (limites.getHeight() - tamano.getHeight()) / 2 - tamano.getMinY());
            }
        }
    }
}
