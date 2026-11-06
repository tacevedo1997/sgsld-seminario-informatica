package sgsld.vista.fx;

import java.util.Arrays;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import sgsld.App;
import sgsld.util.CredencialesInvalidasException;
import sgsld.util.PersistenciaException;

/**
 * Inicio de sesión (CU-09, {@code login.fxml}). Al mostrarse conecta con la
 * base de datos fuera del hilo de JavaFX; si no puede, ofrece reintentar o
 * salir. Ante credenciales inválidas muestra siempre el mismo mensaje
 * genérico, sin indicar si falló el usuario o la contraseña.
 */
public class LoginController extends ControladorPantalla {

    @FXML
    private TextField campoUsuario;
    @FXML
    private PasswordField campoContrasena;
    @FXML
    private Button botonIngresar;
    @FXML
    private Label etiquetaError;
    @FXML
    private Label etiquetaEstado;
    @FXML
    private VBox contenedorAvisos;

    private final BooleanProperty conectado = new SimpleBooleanProperty(false);

    @FXML
    private void initialize() {
        campoUsuario.disableProperty().bind(conectado.not());
        campoContrasena.disableProperty().bind(conectado.not());
        botonIngresar.disableProperty().bind(conectado.not().or(ocupadoProperty()));
        etiquetaError.managedProperty().bind(etiquetaError.visibleProperty());
        for (String aviso : contexto().getAvisos()) {
            Label etiqueta = new Label(aviso);
            etiqueta.getStyleClass().add("nota");
            etiqueta.setWrapText(true);
            contenedorAvisos.getChildren().add(etiqueta);
        }
    }

    @Override
    public String getTitulo() {
        return "Iniciar sesión";
    }

    /** Conecta con la base, salvo que ya haya conexión (por ejemplo, al cerrar sesión). */
    @Override
    public void alMostrar() {
        if (contexto().estaConectado()) {
            listo();
        } else {
            conectar();
        }
    }

    private void conectar() {
        etiquetaEstado.setText("Conectando con la base de datos…");
        ejecutar(() -> {
            contexto().conectar();
            return null;
        }, sinResultado -> listo(), error -> {
            if (error instanceof PersistenciaException persistencia) {
                etiquetaEstado.setText("Sin conexión con la base de datos.");
                if (DialogoErrorConexion.preguntarReintentar(ventana(), persistencia.getMessage())) {
                    conectar();
                } else {
                    Platform.exit();
                }
            } else {
                informarError(error);
            }
        });
    }

    private void listo() {
        etiquetaEstado.setText(App.describirDatos(contexto().getModo()));
        conectado.set(true);
        campoUsuario.requestFocus();
    }

    /** Autentica fuera del hilo de JavaFX (PBKDF2 demora) y abre la ventana principal. */
    @FXML
    private void ingresar() {
        String nombreUsuario = campoUsuario.getText();
        char[] contrasena = campoContrasena.getText().toCharArray();
        etiquetaError.setVisible(false);
        ejecutar(() -> {
            try {
                return servicios().autenticacion().iniciarSesion(nombreUsuario, contrasena);
            } finally {
                Arrays.fill(contrasena, '\0');
            }
        }, usuario -> {
            campoContrasena.clear();
            contexto().mostrarPrincipal();
        }, error -> {
            if (error instanceof CredencialesInvalidasException) {
                etiquetaError.setText(error.getMessage());
                etiquetaError.setVisible(true);
                campoContrasena.clear();
                campoContrasena.requestFocus();
            } else {
                informarError(error);
            }
        });
    }
}
