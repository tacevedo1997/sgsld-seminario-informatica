package sgsld.vista.fx;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import sgsld.modelo.Categoria;
import sgsld.util.SgsldException;
import sgsld.util.Texto;

/**
 * Gestión de categorías (CU-02, {@code categorias.fxml}). Rol: Coordinador.
 * Alta y modificación con nombre único y rango etario; el filtro busca por
 * nombre de forma secuencial sobre la lista leída.
 */
public class CategoriasController extends ControladorAdministracion<Categoria> {

    /** Encabezados de la tabla (cantidad fija). */
    private static final String[] ENCABEZADOS = {"Nombre", "Edad mínima", "Edad máxima", "Rango etario"};

    @FXML
    private TextField campoBusqueda;
    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoEdadMinima;
    @FXML
    private TextField campoEdadMaxima;

    private List<Categoria> todas = new ArrayList<>();

    @Override
    public String getTitulo() {
        return "Categorías";
    }

    @Override
    protected void configurar() {
        TableView<Categoria> tabla = tabla();
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[0], Categoria::getNombre));
        tabla.getColumns().add(Tablas.columnaNumerica(ENCABEZADOS[1],
                categoria -> String.valueOf(categoria.getEdadMin())));
        tabla.getColumns().add(Tablas.columnaNumerica(ENCABEZADOS[2],
                categoria -> String.valueOf(categoria.getEdadMax())));
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[3], Categoria::getRangoEtario));
        Tablas.distribuirAnchos(tabla, 34, 22, 22, 22);
        tabla.setPlaceholder(new Label("No hay categorías para mostrar."));
        Campos.soloDigitos(campoEdadMinima, 2);
        Campos.soloDigitos(campoEdadMaxima, 2);
        campoBusqueda.textProperty().addListener((propiedad, anterior, nuevo) -> filtrar(null));
    }

    @Override
    protected void cargar(Integer idASeleccionar) {
        ejecutar(() -> servicios().categorias().listar(), categorias -> {
            todas = categorias;
            filtrar(idASeleccionar);
        });
    }

    /** Búsqueda secuencial por nombre, sin tildes ni mayúsculas. */
    private void filtrar(Integer idASeleccionar) {
        String texto = Campos.leerTexto(campoBusqueda);
        List<Categoria> visibles = new ArrayList<>();
        for (Categoria categoria : todas) {
            if (texto.isEmpty() || Texto.contiene(categoria.getNombre(), texto)) {
                visibles.add(categoria);
            }
        }
        mostrarLista(visibles, idASeleccionar);
    }

    @Override
    protected String tituloAlta() {
        return "NUEVA CATEGORÍA";
    }

    @Override
    protected String describir(Categoria categoria) {
        return categoria.getNombre() + " (" + categoria.getRangoEtario() + ")";
    }

    @Override
    protected String textoRegistrado() {
        return "Categoría registrada";
    }

    @Override
    protected void mostrarEnFormulario(Categoria categoria) {
        campoNombre.setText(categoria.getNombre());
        campoEdadMinima.setText(String.valueOf(categoria.getEdadMin()));
        campoEdadMaxima.setText(String.valueOf(categoria.getEdadMax()));
    }

    @Override
    protected void limpiarFormulario() {
        campoNombre.clear();
        campoEdadMinima.clear();
        campoEdadMaxima.clear();
    }

    @Override
    protected Categoria armarCandidato(int id, List<String> errores) {
        Integer minima = Campos.leerEntero(campoEdadMinima);
        Integer maxima = Campos.leerEntero(campoEdadMaxima);
        if (minima == null) {
            errores.add("El campo edad mínima es obligatorio.");
        }
        if (maxima == null) {
            errores.add("El campo edad máxima es obligatorio.");
        }
        return new Categoria(id, Campos.leerTexto(campoNombre), minima == null ? 0 : minima,
                maxima == null ? 0 : maxima);
    }

    @Override
    protected int registrar(Categoria candidato) throws SgsldException {
        return servicios().categorias().registrar(candidato);
    }

    @Override
    protected void modificar(Categoria candidato) throws SgsldException {
        servicios().categorias().modificar(candidato);
    }
}
