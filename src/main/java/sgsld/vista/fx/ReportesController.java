package sgsld.vista.fx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.servicio.reporte.Reporte;
import sgsld.util.ExportadorCSV;
import sgsld.util.Fechas;
import sgsld.util.RegistroTecnico;

/**
 * Reportes estadísticos (CU-07, Figura 13, {@code reportes.fxml}). Rol: Coordinador.
 * <p>
 * Los criterios son los seis reportes de la jerarquía {@link Reporte}, que el
 * servicio entrega como una lista: la pantalla los genera y muestra sin
 * conocer su clase concreta (polimorfismo). La tabla y el CSV salen de la
 * misma matriz ({@link Reporte#aMatriz(List)}), con la fila Total en los
 * conteos; el gráfico de barras horizontal muestra el valor de cada fila.
 */
public class ReportesController extends ControladorPantalla {

    private static final String SIN_RESULTADOS = "No se encontraron resultados para los filtros seleccionados.";
    private static final PseudoClass TOTAL = PseudoClass.getPseudoClass("total");
    private static final double ALTO_ENCABEZADO_DOBLE = 60;

    @FXML
    private DatePicker campoDesde;
    @FXML
    private DatePicker campoHasta;
    @FXML
    private ComboBox<Reporte> comboCriterio;
    @FXML
    private Button botonGenerar;
    @FXML
    private Button botonExportar;
    @FXML
    private HBox resultados;
    @FXML
    private TableView<String[]> tabla;
    @FXML
    private Label tituloGrafico;
    @FXML
    private StackPane contenedorGrafico;
    @FXML
    private Label etiquetaSinResultados;

    /** Último reporte generado, para exportarlo. */
    private Generado generado;

    private record Generado(Reporte reporte, LocalDate desde, LocalDate hasta, String[][] matriz) {
    }

    @FXML
    private void initialize() {
        Campos.configurarFecha(campoDesde, hoy());
        Campos.configurarFecha(campoHasta, hoy());
        Campos.mostrarFecha(campoDesde, hoy().withDayOfYear(1));
        Campos.mostrarFecha(campoHasta, hoy());
        Campos.mostrarCon(comboCriterio, Reporte::getTitulo);
        comboCriterio.valueProperty().addListener((propiedad, anterior, reporte) -> {
            boolean sinPeriodo = reporte != null && !reporte.usaPeriodo();
            campoDesde.setDisable(sinPeriodo);
            campoHasta.setDisable(sinPeriodo);
        });
        botonGenerar.disableProperty().bind(ocupadoProperty());
        botonExportar.setDisable(true);
        tabla.getStyleClass().add("tabla-reporte");
        tabla.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(String[] fila, boolean vacia) {
                super.updateItem(fila, vacia);
                pseudoClassStateChanged(TOTAL, !vacia && fila != null && esFilaTotal(fila));
            }
        });
        resultados.managedProperty().bind(resultados.visibleProperty());
        etiquetaSinResultados.managedProperty().bind(etiquetaSinResultados.visibleProperty());
        resultados.setVisible(false);
        etiquetaSinResultados.setVisible(false);
    }

    @Override
    public String getTitulo() {
        return "Reportes estadísticos";
    }

    /** Carga los criterios disponibles y genera el primero con el período por defecto (año en curso). */
    @Override
    public void alMostrar() {
        ejecutar(() -> servicios().reportes().getReportesDisponibles(), reportes -> {
            comboCriterio.getItems().setAll(reportes);
            if (!reportes.isEmpty()) {
                comboCriterio.setValue(reportes.get(0));
                generar();
            }
        });
    }

    /** Genera el reporte elegido; el servicio valida el período (desde no posterior a hasta). */
    @FXML
    private void generar() {
        Reporte reporte = comboCriterio.getValue();
        List<String> errores = new ArrayList<>();
        if (reporte == null) {
            errores.add("Debe elegir un criterio.");
        }
        boolean usaPeriodo = reporte == null || reporte.usaPeriodo();
        LocalDate desde = usaPeriodo ? Campos.leerFecha(campoDesde, "fecha desde", errores) : null;
        LocalDate hasta = usaPeriodo ? Campos.leerFecha(campoHasta, "fecha hasta", errores) : null;
        if (!errores.isEmpty()) {
            mostrarErrores(errores);
            return;
        }
        ejecutar(() -> servicios().reportes().generar(reporte, desde, hasta),
                filas -> mostrar(reporte, desde, hasta, filas));
    }

    private void mostrar(Reporte reporte, LocalDate desde, LocalDate hasta, List<FilaReporteDTO> filas) {
        if (filas.isEmpty()) {
            generado = null;
            botonExportar.setDisable(true);
            resultados.setVisible(false);
            etiquetaSinResultados.setText(SIN_RESULTADOS);
            etiquetaSinResultados.setVisible(true);
            return;
        }
        String[][] matriz = reporte.aMatriz(filas);
        generado = new Generado(reporte, desde, hasta, matriz);
        mostrarTabla(matriz);
        mostrarGrafico(reporte, filas, desde, hasta);
        etiquetaSinResultados.setVisible(false);
        resultados.setVisible(true);
        botonExportar.setDisable(false);
    }

    /** Arma las columnas desde los encabezados de la matriz (arreglo → columnas de la tabla). */
    private void mostrarTabla(String[][] matriz) {
        String[] encabezados = matriz[0];
        tabla.getColumns().clear();
        for (int columna = 0; columna < encabezados.length; columna++) {
            int indice = columna;
            TableColumn<String[], String> nueva = new TableColumn<>(encabezados[columna]);
            nueva.setCellValueFactory(celda -> new ReadOnlyStringWrapper(celda.getValue()[indice]));
            nueva.setSortable(false);
            nueva.setReorderable(false);
            if (columna > 0) {
                nueva.getStyleClass().add("columna-numero");
            }
            tabla.getColumns().add(nueva);
        }
        boolean tresColumnas = encabezados.length > 2;
        tabla.setPrefWidth(tresColumnas ? 410 : 300);
        tabla.setMinWidth(tabla.getPrefWidth());
        tabla.getStyleClass().remove("encabezado-doble");
        if (tresColumnas) {
            tabla.getStyleClass().add("encabezado-doble");
            Tablas.distribuirAnchos(tabla, 40, 30, 30);
        } else {
            Tablas.distribuirAnchos(tabla, 60, 40);
        }
        List<String[]> filas = new ArrayList<>();
        for (int fila = 1; fila < matriz.length; fila++) {
            filas.add(matriz[fila]);
        }
        tabla.getItems().setAll(filas);
        Tablas.ajustarAltura(tabla, 1, 12, Tablas.ALTO_FILA_COMPACTA,
                tresColumnas ? ALTO_ENCABEZADO_DOBLE : Tablas.ALTO_ENCABEZADO);
    }

    private void mostrarGrafico(Reporte reporte, List<FilaReporteDTO> filas, LocalDate desde, LocalDate hasta) {
        List<String> categorias = new ArrayList<>();
        List<Double> valores = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        for (FilaReporteDTO fila : filas) {
            List<String> celdas = reporte.getCeldas(fila);
            categorias.add(fila.etiqueta());
            valores.add(reporte.getValorGrafico(fila));
            textos.add(celdas.get(celdas.size() - 1));
        }
        GraficoBarras grafico = new GraficoBarras();
        grafico.mostrar(categorias, valores, textos, filas.get(0).promedio() == null);
        contenedorGrafico.getChildren().setAll(grafico);
        tituloGrafico.setText(reporte.getTitulo() + " · " + (reporte.usaPeriodo()
                ? Fechas.formatear(desde) + " – " + Fechas.formatear(hasta) : "situación actual"));
    }

    private boolean esFilaTotal(String[] fila) {
        return generado != null && generado.reporte().muestraTotal()
                && fila == generado.matriz()[generado.matriz().length - 1];
    }

    /** Exporta el último reporte generado a CSV (UTF-8 con BOM y punto y coma, para Excel). */
    @FXML
    private void exportar() {
        if (generado == null) {
            return;
        }
        Reporte reporte = generado.reporte();
        FileChooser selector = new FileChooser();
        selector.setTitle("SGSLD — Exportar reporte a CSV");
        selector.setInitialFileName(reporte.usaPeriodo()
                ? ExportadorCSV.nombreSugerido(reporte.getTitulo(), generado.desde(), generado.hasta())
                : ExportadorCSV.nombreSugerido(reporte.getTitulo(), hoy(), null));
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV (*.csv)", "*.csv"));
        File elegido = contexto().elegirArchivoParaGuardar(selector);
        if (elegido == null) {
            return;
        }
        Path archivo = elegido.getName().toLowerCase(Locale.ROOT).endsWith(".csv")
                ? elegido.toPath() : elegido.toPath().resolveSibling(elegido.getName() + ".csv");
        try {
            ExportadorCSV.exportar(generado.matriz(), archivo);
            mostrarInfo("Reporte exportado a:\n" + archivo.toAbsolutePath());
        } catch (IOException e) {
            RegistroTecnico.logger().log(Level.WARNING, "No se pudo exportar el reporte a " + archivo, e);
            mostrarError("No se pudo guardar el archivo " + archivo.getFileName()
                    + ". Verifique que no esté abierto en otro programa y que la carpeta permita escribir.");
        }
    }
}
