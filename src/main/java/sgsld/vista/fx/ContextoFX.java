package sgsld.vista.fx;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Level;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import sgsld.App;
import sgsld.servicio.Servicios;
import sgsld.util.PersistenciaException;
import sgsld.util.RegistroTecnico;

/**
 * Estado compartido de la interfaz JavaFX: la ventana, el reloj (respeta
 * {@code -Dsgsld.hoy}), el modo de datos, los avisos de arranque, el ejecutor
 * de las tareas contra la base y los servicios, que existen recién cuando hay
 * conexión. También carga las pantallas FXML e inyecta este contexto en sus
 * controladores.
 */
public final class ContextoFX {

    /** Ancho mínimo de la ventana (sección 6.2). */
    public static final double ANCHO_MINIMO = 1100;
    /** Alto mínimo de la ventana (sección 6.2). */
    public static final double ALTO_MINIMO = 720;

    private static final String HOJA_DE_ESTILO = "sgsld.css";

    private final Stage ventana;
    private final Clock reloj;
    private final App.ModoDatos modo;
    private final List<String> avisos;
    private final Executor ejecutor;
    /** Se asigna en el hilo de datos y se lee en el de JavaFX. */
    private volatile Servicios servicios;
    private Pantalla pantallaActual;
    private int tareasEnCurso;
    /** Cómo se muestran los diálogos: modales, salvo en el Capturador. */
    private Function<Dialog<ButtonType>, Optional<ButtonType>> presentador = Dialog::showAndWait;
    /** Cómo se elige dónde guardar un archivo: con el diálogo del sistema, salvo en el Capturador. */
    private Function<FileChooser, File> selectorDeArchivos;

    /**
     * Crea el contexto y prepara la ventana: escena con la hoja de estilo y
     * tamaño mínimo.
     *
     * @param ventana  ventana principal
     * @param reloj    reloj de la aplicación
     * @param modo     origen de los datos
     * @param avisos   avisos de arranque (fecha fijada, modo de datos...)
     * @param ejecutor ejecutor de las tareas que acceden a los datos
     */
    ContextoFX(Stage ventana, Clock reloj, App.ModoDatos modo, List<String> avisos, Executor ejecutor) {
        this.ventana = ventana;
        this.reloj = reloj;
        this.modo = modo;
        this.avisos = List.copyOf(avisos);
        this.ejecutor = ejecutor;
        this.selectorDeArchivos = selector -> selector.showSaveDialog(ventana);
        Scene escena = new Scene(new StackPane(), ANCHO_MINIMO, ALTO_MINIMO);
        escena.getStylesheets().add(hojaDeEstilo());
        ventana.setScene(escena);
        ventana.setMinWidth(ANCHO_MINIMO);
        ventana.setMinHeight(ALTO_MINIMO);
    }

    /**
     * URL de {@code sgsld.css}, para la escena y los diálogos.
     *
     * @return la URL en forma de texto
     */
    static String hojaDeEstilo() {
        return ContextoFX.class.getResource(HOJA_DE_ESTILO).toExternalForm();
    }

    public Stage getVentana() {
        return ventana;
    }

    public Clock getReloj() {
        return reloj;
    }

    public App.ModoDatos getModo() {
        return modo;
    }

    public List<String> getAvisos() {
        return avisos;
    }

    Executor getEjecutor() {
        return ejecutor;
    }

    /**
     * Fecha actual según el reloj de la aplicación.
     *
     * @return la fecha de hoy
     */
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /**
     * Indica si ya hay servicios (la conexión con la base se estableció).
     *
     * @return {@code true} si hay conexión
     */
    public boolean estaConectado() {
        return servicios != null;
    }

    /**
     * Servicios de la aplicación.
     *
     * @return los servicios
     * @throws IllegalStateException si todavía no hay conexión
     */
    public Servicios getServicios() {
        if (servicios == null) {
            throw new IllegalStateException("Todavía no hay conexión con la base de datos");
        }
        return servicios;
    }

    /**
     * Crea la fábrica de DAO del modo de datos (en MySQL lee
     * {@code db.properties} y prueba la conexión) y arma los servicios. Se
     * ejecuta fuera del hilo de JavaFX.
     *
     * @throws PersistenciaException si falta la configuración o MySQL no responde
     */
    void conectar() throws PersistenciaException {
        servicios = Servicios.crear(App.crearFabrica(modo), reloj);
    }

    /**
     * Registra el inicio de una tarea contra los datos: mientras haya alguna,
     * el cursor es de espera.
     */
    void tareaIniciada() {
        if (tareasEnCurso++ == 0) {
            ventana.getScene().setCursor(Cursor.WAIT);
        }
    }

    /** Registra el fin de una tarea contra los datos. */
    void tareaTerminada() {
        if (--tareasEnCurso == 0) {
            ventana.getScene().setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Indica si hay tareas cuyo resultado todavía no llegó al hilo de JavaFX.
     *
     * @return {@code true} si hay alguna en curso
     */
    boolean hayTareasEnCurso() {
        return tareasEnCurso > 0;
    }

    /**
     * Muestra un diálogo y devuelve el botón elegido. En la aplicación es
     * modal ({@code showAndWait}).
     *
     * @param dialogo diálogo armado
     * @return el botón elegido, o vacío si se cerró sin elegir
     */
    Optional<ButtonType> presentar(Dialog<ButtonType> dialogo) {
        return presentador.apply(dialogo);
    }

    /**
     * Cambia la forma de mostrar los diálogos (lo usa el Capturador, que los
     * guarda como imagen y elige el botón por defecto sin esperar al usuario).
     *
     * @param presentador función que muestra el diálogo y devuelve la respuesta
     */
    void usarPresentador(Function<Dialog<ButtonType>, Optional<ButtonType>> presentador) {
        this.presentador = presentador;
    }

    /**
     * Pide dónde guardar un archivo.
     *
     * @param selector diálogo con el título, la carpeta y el nombre sugerido
     * @return el archivo elegido, o {@code null} si se canceló
     */
    File elegirArchivoParaGuardar(FileChooser selector) {
        return selectorDeArchivos.apply(selector);
    }

    /**
     * Cambia la forma de elegir dónde guardar (lo usa el Capturador).
     *
     * @param selectorDeArchivos función que devuelve el archivo elegido
     */
    void usarSelectorDeArchivos(Function<FileChooser, File> selectorDeArchivos) {
        this.selectorDeArchivos = selectorDeArchivos;
    }

    /**
     * Pantalla que ocupa la ventana (inicio de sesión o principal).
     *
     * @return la pantalla actual
     */
    Pantalla getPantallaActual() {
        return pantallaActual;
    }

    /**
     * Abre otra opción del menú lateral (por ejemplo, Tratamientos después de
     * registrar una lesión).
     *
     * @param archivoFxml pantalla de la opción
     * @return su controlador, o {@code null} si no hay ventana principal o el rol no tiene esa opción
     */
    ControladorPantalla abrirOpcion(String archivoFxml) {
        if (pantallaActual != null && pantallaActual.controlador() instanceof PrincipalController principal) {
            return principal.abrir(archivoFxml);
        }
        return null;
    }

    /** Muestra la pantalla de inicio de sesión. */
    public void mostrarLogin() {
        mostrar("login.fxml");
    }

    /** Muestra la ventana principal con el menú del rol de la sesión. */
    public void mostrarPrincipal() {
        mostrar("principal.fxml");
    }

    /**
     * Pone el título de la ventana: "SGSLD — pantalla".
     *
     * @param pantalla título de la pantalla actual
     */
    void actualizarTitulo(String pantalla) {
        ventana.setTitle("SGSLD — " + pantalla);
    }

    private void mostrar(String archivo) {
        try {
            Pantalla pantalla = cargar(archivo);
            pantallaActual = pantalla;
            ventana.getScene().setRoot(pantalla.raiz());
            actualizarTitulo(pantalla.controlador().getTitulo());
            pantalla.controlador().alMostrar();
        } catch (IOException e) {
            RegistroTecnico.logger().log(Level.SEVERE, "No se pudo cargar " + archivo, e);
            Dialogos.error(ventana, "No se pudo abrir la pantalla. El detalle quedó en el registro técnico.");
            Platform.exit();
        }
    }

    /**
     * Carga un archivo FXML de {@code sgsld/vista/fx/}. La fábrica de
     * controladores les inyecta este contexto antes de que FXML complete sus
     * campos, de modo que {@code initialize()} ya puede usarlo.
     *
     * @param archivo nombre del archivo, por ejemplo {@code estado_actual.fxml}
     * @return la raíz y su controlador
     * @throws IOException si el archivo no existe o es inválido
     */
    Pantalla cargar(String archivo) throws IOException {
        URL recurso = ContextoFX.class.getResource(archivo);
        if (recurso == null) {
            throw new IOException("No se encontró la pantalla " + archivo);
        }
        FXMLLoader cargador = new FXMLLoader(recurso);
        cargador.setControllerFactory(this::crearControlador);
        Parent raiz = cargador.load();
        Object controlador = cargador.getController();
        if (!(controlador instanceof ControladorPantalla pantalla)) {
            throw new IOException(archivo + " no declara un ControladorPantalla");
        }
        return new Pantalla(raiz, pantalla);
    }

    private Object crearControlador(Class<?> tipo) {
        try {
            Object controlador = tipo.getDeclaredConstructor().newInstance();
            if (controlador instanceof ControladorPantalla pantalla) {
                pantalla.asignarContexto(this);
            }
            return controlador;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo crear el controlador " + tipo.getName(), e);
        }
    }

    /**
     * Pantalla cargada desde FXML.
     *
     * @param raiz        nodo raíz
     * @param controlador su controlador
     */
    record Pantalla(Parent raiz, ControladorPantalla controlador) {
    }
}
