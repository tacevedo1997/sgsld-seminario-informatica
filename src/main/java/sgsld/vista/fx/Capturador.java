package sgsld.vista.fx;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;

import sgsld.App;
import sgsld.util.SgsldException;
import sgsld.util.Texto;

/**
 * Herramienta de desarrollo, fuera del flujo normal de la aplicación: abre
 * cada pantalla con el lote de prueba en memoria y la fecha fija del plan de
 * pruebas (26/09/2026), guarda una imagen con {@code snapshot()} y termina.
 * Sirve para comparar las pantallas con los prototipos del informe.
 * <p>
 * Además recorre algunos escenarios como lo haría un usuario (completa campos
 * y presiona botones) y verifica el resultado: alta con errores y alta
 * correcta, baja lógica, nombre duplicado y contraseñas distintas. Los
 * diálogos no esperan al usuario: se guardan como imagen y se responde con el
 * botón por defecto.
 * <p>
 * Todo ocurre en el hilo de JavaFX: las tareas de las pantallas se ejecutan en
 * el mismo hilo y, antes de cada imagen, se procesan los eventos pendientes
 * hasta que llegan todos sus resultados ({@link #esperarTareas()}).
 * <p>
 * Uso: {@code java -cp target/sgsld.jar sgsld.vista.fx.Capturador [carpeta]}
 * (por defecto, {@code target/capturas}). Con {@code -Dsgsld.datos=mysql} y
 * {@code -Dsgsld.config=db-test.properties} recorre los mismos escenarios
 * sobre una base {@code sgsld_test} recién creada.
 */
public final class Capturador {

    /** Contraseña de los usuarios del lote de prueba (sgsld_script.sql). */
    private static final String CONTRASENA_LOTE = "Sgsld2026!";
    private static final String[] USUARIOS = {"mlopez", "lfernandez", "ctecnico"};
    private static final double ESCALA = 2;
    /** Mensaje de ejemplo para la captura del diálogo de error de conexión. */
    private static final String MENSAJE_SIN_SERVIDOR = "No se pudo conectar con el servidor MySQL. "
            + "Verifique que el servicio esté en ejecución y que la dirección de db.properties sea correcta.";

    private final Path destino;
    private final List<Path> guardadas = new ArrayList<>();
    private final List<String> dialogos = new ArrayList<>();
    private ContextoFX contexto;
    private Stage ventana;
    private int numero = 1;
    private String nombreDialogo = "dialogo";

    private Capturador(Path destino) {
        this.destino = destino;
    }

    /**
     * Genera las capturas y recorre los escenarios.
     *
     * @param args carpeta de destino (opcional)
     * @throws Exception si falla alguna captura o un escenario no da el resultado esperado
     */
    public static void main(String[] args) throws Exception {
        if (System.getProperty(App.PROPIEDAD_DATOS) == null) {
            System.setProperty(App.PROPIEDAD_DATOS, "memoria");
        }
        if (System.getProperty("sgsld.hoy") == null) {
            System.setProperty("sgsld.hoy", "2026-09-26");
        }
        Path destino = args.length > 0 ? Path.of(args[0]) : Path.of("target", "capturas");
        Files.createDirectories(destino);
        // Solo las capturas anteriores de esta herramienta (nombres "NN-...png").
        try (var anteriores = Files.newDirectoryStream(destino, "[0-9][0-9]-*.png")) {
            for (Path anterior : anteriores) {
                Files.delete(anterior);
            }
        }
        Locale.setDefault(Texto.LOCALE);
        Lanzador.silenciarAvisoDeConfiguracion();
        Capturador capturador = new Capturador(destino);
        CompletableFuture<List<Path>> resultado = new CompletableFuture<>();
        Platform.startup(() -> {
            try {
                resultado.complete(capturador.capturar());
            } catch (Exception | Error e) {
                resultado.completeExceptionally(e);
            }
        });
        try {
            for (Path archivo : resultado.get()) {
                System.out.println("Captura: " + archivo);
            }
        } finally {
            Platform.exit();
        }
    }

    private List<Path> capturar() throws IOException, SgsldException {
        List<String> avisos = new ArrayList<>();
        Clock reloj = App.crearReloj(avisos);
        App.ModoDatos modo = App.modoDatos(avisos);
        ventana = new Stage();
        contexto = new ContextoFX(ventana, reloj, modo, avisos, Runnable::run);
        contexto.usarPresentador(this::presentar);
        contexto.conectar();

        contexto.mostrarLogin();
        ventana.show();
        esperarTareas();
        guardar("login");
        recorrerPantallas();
        guardar(DialogoErrorConexion.crear(ventana, MENSAJE_SIN_SERVIDOR), "error-conexion");

        iniciarSesion("mlopez");
        escenarioJugadores();
        escenarioCategorias();
        escenarioKinesiologos();
        cerrarSesion();
        ventana.close();
        return guardadas;
    }

    /** Cada usuario del lote abre todas sus opciones; en administración, también una fila elegida. */
    private void recorrerPantallas() throws SgsldException {
        for (String usuario : USUARIOS) {
            PrincipalController principal = iniciarSesion(usuario);
            for (OpcionMenu opcion : principal.opcionesHabilitadas()) {
                ControladorPantalla pantalla = principal.abrir(opcion);
                esperarTareas();
                String nombre = usuario + "-" + opcion.archivoFxml().replace(".fxml", "").replace('_', '-');
                guardar(nombre);
                if (pantalla instanceof ControladorAdministracion<?> administracion) {
                    administracion.seleccionarFila(0);
                    guardar(nombre + "-modificar");
                }
            }
            cerrarSesion();
        }
    }

    // ---------- Escenarios ----------

    /** CU-01: alta con errores (todos juntos), alta correcta y baja lógica con confirmación. */
    private void escenarioJugadores() throws SgsldException {
        abrir("Jugadores");
        presionar("botonNuevo");
        escribir("campoDni", "42587634");
        escribir("campoApellido", "Suárez");
        escribir("campoNombre", "Martín");
        escribirFecha("campoNacimiento", "10/05/2001");
        guardarConDialogo("botonGuardar", "jugadores-alta-con-errores");
        exigirDialogo("Ya existe un jugador con DNI 42587634.", "El campo posición es obligatorio.",
                "El campo categoría es obligatorio.");

        escribir("campoDni", "43111222");
        escribir("campoCamiseta", "6");
        escribir("campoPosicion", "Defensor");
        elegir("comboCategoria", 0);
        presionar("botonGuardar");
        esperarTareas();
        guardar("jugadores-alta-correcta");
        exigir(tablaContiene("43111222"), "el jugador nuevo no aparece en la tabla");
        exigir(textoDe("etiquetaResultado").startsWith("Jugador registrado con el N° 9"), "falta el mensaje de alta");

        guardarConDialogo("botonBaja", "jugadores-confirmar-baja");
        exigirDialogo("¿Dar de baja a Suárez, Martín (DNI 43111222)?");
        guardar("jugadores-baja");
        exigir(!tablaContiene("43111222"), "el jugador dado de baja sigue en la tabla");
        exigir(textoDe("tituloFormulario").equals("NUEVO JUGADOR"), "el formulario sigue mostrando al dado de baja");
        marcar("checkIncluirBaja");
        esperarTareas();
        exigir(tablaContiene("Dado de baja"), "\"Incluir dados de baja\" no muestra al jugador");
        guardar("jugadores-incluir-dados-de-baja");
    }

    /** CU-02: nombre repetido (sin distinguir mayúsculas) y alta correcta. */
    private void escenarioCategorias() throws SgsldException {
        abrir("Categorías");
        presionar("botonNuevo");
        escribir("campoNombre", "PRIMERA");
        escribir("campoEdadMinima", "18");
        escribir("campoEdadMaxima", "40");
        guardarConDialogo("botonGuardar", "categorias-nombre-repetido");
        exigirDialogo("Ya existe una categoría con el nombre Primera.");
        escribir("campoNombre", "Sub-19");
        escribir("campoEdadMaxima", "19");
        presionar("botonGuardar");
        esperarTareas();
        exigir(tablaContiene("Sub-19"), "la categoría nueva no aparece en la tabla");
        guardar("categorias-alta-correcta");
    }

    /** CU-03: contraseñas distintas y alta con usuario del sistema (en una transacción). */
    private void escenarioKinesiologos() throws SgsldException {
        abrir("Kinesiólogos");
        presionar("botonNuevo");
        escribir("campoApellido", "Ríos");
        escribir("campoNombre", "Ana");
        escribir("campoMatricula", "MP-13001");
        marcar("checkCrearUsuario");
        escribir("campoUsuario", "arios");
        escribir("campoContrasena", "clave1234");
        escribir("campoRepeticion", "clave4321");
        guardar("kinesiologos-alta-con-usuario");
        guardarConDialogo("botonGuardar", "kinesiologos-contrasenas-distintas");
        exigirDialogo("Las contraseñas no coinciden.");
        escribir("campoRepeticion", "clave1234");
        presionar("botonGuardar");
        esperarTareas();
        exigir(tablaContiene("arios"), "el kinesiólogo nuevo no aparece con su usuario");
        guardar("kinesiologos-alta-correcta");
    }

    // ---------- Acciones de usuario ----------

    private PrincipalController iniciarSesion(String usuario) throws SgsldException {
        contexto.getServicios().autenticacion().iniciarSesion(usuario, CONTRASENA_LOTE.toCharArray());
        contexto.mostrarPrincipal();
        esperarTareas();
        return principal();
    }

    private void cerrarSesion() {
        contexto.getServicios().autenticacion().cerrarSesion();
    }

    private PrincipalController principal() {
        return (PrincipalController) contexto.getPantallaActual().controlador();
    }

    private void abrir(String textoMenu) {
        for (OpcionMenu opcion : principal().opcionesHabilitadas()) {
            if (opcion.texto().equals(textoMenu)) {
                principal().abrir(opcion);
                esperarTareas();
                return;
            }
        }
        throw new IllegalStateException("El menú no tiene la opción " + textoMenu);
    }

    /** Busca un control por su fx:id; antes aplica CSS para que existan las pieles (el ScrollPane, por ejemplo). */
    private <T extends Node> T buscar(String id, Class<T> tipo) {
        ventana.getScene().getRoot().applyCss();
        ventana.getScene().getRoot().layout();
        Node nodo = ventana.getScene().lookup("#" + id);
        if (!tipo.isInstance(nodo)) {
            throw new IllegalStateException("No se encontró #" + id + " (" + tipo.getSimpleName() + ")");
        }
        return tipo.cast(nodo);
    }

    private void presionar(String id) {
        buscar(id, ButtonBase.class).fire();
    }

    private void marcar(String id) {
        CheckBox casilla = buscar(id, CheckBox.class);
        if (!casilla.isSelected()) {
            casilla.fire();
        }
    }

    private void escribir(String id, String texto) {
        buscar(id, TextInputControl.class).setText(texto);
    }

    private void escribirFecha(String id, String texto) {
        buscar(id, DatePicker.class).getEditor().setText(texto);
    }

    private void elegir(String id, int indice) {
        buscar(id, ComboBox.class).getSelectionModel().select(indice);
    }

    private String textoDe(String id) {
        return buscar(id, Label.class).getText();
    }

    /** Presiona el botón, procesa las tareas y guarda la imagen del diálogo que aparezca. */
    private void guardarConDialogo(String idBoton, String nombre) {
        int vistos = dialogos.size();
        nombreDialogo = nombre;
        presionar(idBoton);
        esperarTareas();
        nombreDialogo = "dialogo";
        exigir(dialogos.size() > vistos, "no apareció el diálogo esperado (" + nombre + ")");
    }

    private void exigirDialogo(String... fragmentos) {
        String ultimo = dialogos.get(dialogos.size() - 1);
        for (String fragmento : fragmentos) {
            exigir(ultimo.contains(fragmento), "el diálogo no dice \"" + fragmento + "\":\n" + ultimo);
        }
    }

    private boolean tablaContiene(String texto) {
        TableView<?> tabla = buscar("tabla", TableView.class);
        for (int fila = 0; fila < tabla.getItems().size(); fila++) {
            for (var columna : tabla.getColumns()) {
                Object valor = columna.getCellData(fila);
                if (valor != null && valor.toString().contains(texto)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void exigir(boolean condicion, String problema) {
        if (!condicion) {
            throw new IllegalStateException("Escenario fallido: " + problema);
        }
    }

    /**
     * Muestra el diálogo sin esperar, guarda su imagen, registra su texto y
     * responde con el botón por defecto (Aceptar, Sí, Reintentar...).
     */
    private Optional<ButtonType> presentar(Dialog<ButtonType> dialogo) {
        dialogo.show();
        dialogos.add(dialogo.getDialogPane().getHeaderText() + "\n" + dialogo.getDialogPane().getContentText());
        guardar(dialogo, nombreDialogo);
        ButtonType respuesta = dialogo.getDialogPane().getButtonTypes().get(0);
        for (ButtonType boton : dialogo.getDialogPane().getButtonTypes()) {
            if (boton.getButtonData().isDefaultButton()) {
                respuesta = boton;
                break;
            }
        }
        dialogo.setResult(respuesta);
        dialogo.close();
        return Optional.of(respuesta);
    }

    /**
     * Procesa los eventos pendientes del hilo de JavaFX (entre ellos, los
     * resultados de las tareas) hasta que no quede ninguna tarea en curso.
     */
    private void esperarTareas() {
        do {
            Object clave = new Object();
            Platform.runLater(() -> Platform.exitNestedEventLoop(clave, null));
            Platform.enterNestedEventLoop(clave);
        } while (contexto.hayTareasEnCurso());
    }

    // ---------- Imágenes ----------

    private void guardar(String nombre) {
        Scene escena = ventana.getScene();
        guardarNodo(escena.getRoot(), nombre);
    }

    private void guardar(Dialog<ButtonType> dialogo, String nombre) {
        if (dialogo instanceof Alert alerta && !alerta.isShowing()) {
            alerta.show();
            guardarNodo(alerta.getDialogPane(), nombre);
            alerta.close();
        } else {
            guardarNodo(dialogo.getDialogPane(), nombre);
        }
    }

    /** Guarda la imagen del nodo en PNG, al doble de resolución, con un número correlativo. */
    private void guardarNodo(Node nodo, String nombre) {
        Path archivo = destino.resolve(String.format("%02d-%s.png", numero++, nombre));
        SnapshotParameters parametros = new SnapshotParameters();
        parametros.setTransform(Transform.scale(ESCALA, ESCALA));
        WritableImage imagen = nodo.snapshot(parametros, null);
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        int[] pixeles = new int[ancho * alto];
        imagen.getPixelReader().getPixels(0, 0, ancho, alto, PixelFormat.getIntArgbInstance(), pixeles, 0, ancho);
        BufferedImage salida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        salida.setRGB(0, 0, ancho, alto, pixeles, 0, ancho);
        try {
            ImageIO.write(salida, "png", archivo.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + archivo, e);
        }
        guardadas.add(archivo);
    }
}
