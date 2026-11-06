package sgsld.vista.fx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import sgsld.modelo.Usuario;
import sgsld.util.RegistroTecnico;
import sgsld.util.SesionUsuario;

/**
 * Ventana principal ({@code principal.fxml}): menú lateral armado según el rol
 * de la sesión y, a la derecha, la pantalla elegida. Las opciones sin acceso
 * del rol se muestran deshabilitadas, como "Reportes (sin acceso)" en la
 * Figura 11.
 */
public class PrincipalController extends ControladorPantalla {

    private static final String ACTIVA = "activa";

    @FXML
    private Label etiquetaUsuario;
    @FXML
    private VBox contenedorOpciones;
    @FXML
    private StackPane contenido;

    private final List<OpcionMenu> opciones = new ArrayList<>();
    private final List<Button> botones = new ArrayList<>();
    private String tituloActual = "Menú principal";

    @FXML
    private void initialize() {
        Usuario usuario = usuario();
        etiquetaUsuario.setText(usuario.getNombreUsuario() + " · " + usuario.getRol());
        SesionUsuario sesion = SesionUsuario.getInstancia();
        for (OpcionMenu opcion : OpcionMenu.paraSesion(sesion)) {
            Button boton = new Button(opcion.textoPara(sesion));
            boton.getStyleClass().add("menu-opcion");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setDisable(!opcion.habilitadaPara(sesion));
            boton.setOnAction(evento -> abrir(opcion));
            opciones.add(opcion);
            botones.add(boton);
            contenedorOpciones.getChildren().add(boton);
        }
    }

    @Override
    public String getTitulo() {
        return tituloActual;
    }

    /** Abre la primera opción habilitada del rol. */
    @Override
    public void alMostrar() {
        List<OpcionMenu> habilitadas = opcionesHabilitadas();
        if (!habilitadas.isEmpty()) {
            abrir(habilitadas.get(0));
        }
    }

    /**
     * Opciones del menú que el rol puede usar.
     *
     * @return las opciones habilitadas, en el orden del menú
     */
    List<OpcionMenu> opcionesHabilitadas() {
        List<OpcionMenu> habilitadas = new ArrayList<>();
        SesionUsuario sesion = SesionUsuario.getInstancia();
        for (OpcionMenu opcion : opciones) {
            if (opcion.habilitadaPara(sesion)) {
                habilitadas.add(opcion);
            }
        }
        return habilitadas;
    }

    /**
     * Marca la opción como activa, carga su pantalla en el área de contenido y
     * le pide que cargue sus datos.
     *
     * @param opcion opción elegida
     */
    void abrir(OpcionMenu opcion) {
        for (int i = 0; i < opciones.size(); i++) {
            botones.get(i).getStyleClass().remove(ACTIVA);
            if (opciones.get(i) == opcion) {
                botones.get(i).getStyleClass().add(ACTIVA);
            }
        }
        try {
            ContextoFX.Pantalla pantalla = contexto().cargar(opcion.archivoFxml());
            ScrollPane desplazable = new ScrollPane(pantalla.raiz());
            desplazable.setFitToWidth(true);
            desplazable.getStyleClass().add("contenido-desplazable");
            contenido.getChildren().setAll(desplazable);
            tituloActual = pantalla.controlador().getTitulo();
            contexto().actualizarTitulo(tituloActual);
            pantalla.controlador().alMostrar();
        } catch (IOException e) {
            RegistroTecnico.logger().log(Level.SEVERE, "No se pudo cargar " + opcion.archivoFxml(), e);
            Label aviso = new Label("No se pudo abrir la pantalla \"" + opcion.texto()
                    + "\". El detalle quedó en el registro técnico.");
            aviso.getStyleClass().add("nota");
            contenido.getChildren().setAll(aviso);
            tituloActual = opcion.texto();
            contexto().actualizarTitulo(tituloActual);
        }
    }

    @FXML
    private void cerrarSesion() {
        servicios().autenticacion().cerrarSesion();
        contexto().mostrarLogin();
    }
}
