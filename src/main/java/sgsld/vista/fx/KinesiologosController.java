package sgsld.vista.fx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;

import sgsld.modelo.Kinesiologo;
import sgsld.servicio.KinesiologoService;
import sgsld.util.SgsldException;
import sgsld.util.Texto;

/**
 * Gestión de kinesiólogos (CU-03, {@code kinesiologos.fxml}). Rol: Coordinador.
 * <p>
 * En el alta se puede crear, en la misma transacción, el usuario del sistema
 * con rol Kinesiólogo (la contraseña se pide dos veces y se guarda solo como
 * hash PBKDF2). La modificación cambia nombre, apellido y matrícula. El
 * filtro busca por apellido, nombre o matrícula de forma secuencial.
 */
public class KinesiologosController extends ControladorAdministracion<Kinesiologo> {

    /** Encabezados de la tabla (cantidad fija). */
    private static final String[] ENCABEZADOS = {"Apellido y nombre", "Matrícula", "Usuario del sistema"};

    @FXML
    private TextField campoBusqueda;
    @FXML
    private TextField campoApellido;
    @FXML
    private TextField campoNombre;
    @FXML
    private TextField campoMatricula;
    @FXML
    private Pane seccionUsuario;
    @FXML
    private CheckBox checkCrearUsuario;
    @FXML
    private TextField campoUsuario;
    @FXML
    private PasswordField campoContrasena;
    @FXML
    private PasswordField campoRepeticion;
    @FXML
    private Label etiquetaUsuarioActual;

    private List<Kinesiologo> todos = new ArrayList<>();
    /** Usuario a crear junto con el kinesiólogo, leído en el hilo de JavaFX ({@code null}: sin usuario). */
    private String usuarioNuevo;
    private char[] contrasenaNueva;

    @Override
    public String getTitulo() {
        return "Kinesiólogos";
    }

    @Override
    protected void configurar() {
        TableView<Kinesiologo> tabla = tabla();
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[0], Kinesiologo::getNombreCompleto));
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[1], Kinesiologo::getMatricula));
        tabla.getColumns().add(Tablas.columna(ENCABEZADOS[2], kinesiologo -> kinesiologo.tieneUsuario()
                ? kinesiologo.getUsuario().getNombreUsuario() : "Sin usuario"));
        Tablas.distribuirAnchos(tabla, 40, 25, 35);
        tabla.setPlaceholder(new Label("No hay kinesiólogos para mostrar."));
        seccionUsuario.managedProperty().bind(seccionUsuario.visibleProperty());
        etiquetaUsuarioActual.managedProperty().bind(etiquetaUsuarioActual.visibleProperty());
        campoUsuario.disableProperty().bind(checkCrearUsuario.selectedProperty().not());
        campoContrasena.disableProperty().bind(checkCrearUsuario.selectedProperty().not());
        campoRepeticion.disableProperty().bind(checkCrearUsuario.selectedProperty().not());
        campoBusqueda.textProperty().addListener((propiedad, anterior, nuevo) -> filtrar(null));
    }

    @Override
    protected void cargar(Integer idASeleccionar) {
        ejecutar(() -> servicios().kinesiologos().listar(), kinesiologos -> {
            todos = kinesiologos;
            filtrar(idASeleccionar);
        });
    }

    /** Búsqueda secuencial por apellido, nombre o matrícula, sin tildes ni mayúsculas. */
    private void filtrar(Integer idASeleccionar) {
        String texto = Campos.leerTexto(campoBusqueda);
        List<Kinesiologo> visibles = new ArrayList<>();
        for (Kinesiologo kinesiologo : todos) {
            if (texto.isEmpty() || Texto.contiene(kinesiologo.getNombreCompleto(), texto)
                    || Texto.contiene(kinesiologo.getMatricula(), texto)) {
                visibles.add(kinesiologo);
            }
        }
        mostrarLista(visibles, idASeleccionar);
    }

    @Override
    protected String tituloAlta() {
        return "NUEVO KINESIÓLOGO";
    }

    @Override
    protected String describir(Kinesiologo kinesiologo) {
        return kinesiologo.getNombreCompleto() + " (" + kinesiologo.getMatricula() + ")";
    }

    @Override
    protected String textoRegistrado() {
        return "Kinesiólogo registrado";
    }

    @Override
    protected void mostrarEnFormulario(Kinesiologo kinesiologo) {
        campoApellido.setText(kinesiologo.getApellido());
        campoNombre.setText(kinesiologo.getNombre());
        campoMatricula.setText(kinesiologo.getMatricula());
        campoContrasena.clear();
        campoRepeticion.clear();
        seccionUsuario.setVisible(false);
        etiquetaUsuarioActual.setText(kinesiologo.tieneUsuario()
                ? "Usuario del sistema: " + kinesiologo.getUsuario().getNombreUsuario()
                : "Sin usuario del sistema (no inicia sesión).");
        etiquetaUsuarioActual.setVisible(true);
    }

    @Override
    protected void limpiarFormulario() {
        campoApellido.clear();
        campoNombre.clear();
        campoMatricula.clear();
        checkCrearUsuario.setSelected(false);
        campoUsuario.clear();
        campoContrasena.clear();
        campoRepeticion.clear();
        seccionUsuario.setVisible(true);
        etiquetaUsuarioActual.setVisible(false);
    }

    /** Lee también el usuario a crear; las contraseñas deben coincidir. */
    @Override
    protected Kinesiologo armarCandidato(int id, List<String> errores) {
        usuarioNuevo = null;
        contrasenaNueva = null;
        if (id == 0 && checkCrearUsuario.isSelected()) {
            char[] contrasena = campoContrasena.getText().toCharArray();
            char[] repeticion = campoRepeticion.getText().toCharArray();
            if (Arrays.equals(contrasena, repeticion)) {
                usuarioNuevo = Campos.leerTexto(campoUsuario);
                contrasenaNueva = contrasena;
            } else {
                errores.add("Las contraseñas no coinciden.");
                Arrays.fill(contrasena, '\0');
            }
            Arrays.fill(repeticion, '\0');
        }
        return new Kinesiologo(id, Campos.leerTexto(campoNombre), Campos.leerTexto(campoApellido),
                Campos.leerTexto(campoMatricula), null);
    }

    @Override
    protected int registrar(Kinesiologo candidato) throws SgsldException {
        KinesiologoService kinesiologos = servicios().kinesiologos();
        if (usuarioNuevo == null) {
            return kinesiologos.registrar(candidato);
        }
        try {
            return kinesiologos.registrar(candidato, usuarioNuevo, contrasenaNueva);
        } finally {
            Arrays.fill(contrasenaNueva, '\0');
        }
    }

    @Override
    protected void modificar(Kinesiologo candidato) throws SgsldException {
        servicios().kinesiologos().modificar(candidato);
    }
}
