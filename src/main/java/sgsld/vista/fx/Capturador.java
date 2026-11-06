package sgsld.vista.fx;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
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
 * Todo ocurre en el hilo de JavaFX: las tareas de las pantallas se ejecutan en
 * el mismo hilo y, antes de cada imagen, se procesan los eventos pendientes
 * hasta que llegan todos sus resultados ({@link #esperarTareas(ContextoFX)}).
 * <p>
 * Uso: {@code java -cp target/sgsld.jar sgsld.vista.fx.Capturador [carpeta]}
 * (por defecto, {@code target/capturas}).
 */
public final class Capturador {

    /** Contraseña de los usuarios del lote de prueba (sgsld_script.sql). */
    private static final String CONTRASENA_LOTE = "Sgsld2026!";
    private static final String[] USUARIOS = {"mlopez", "lfernandez", "ctecnico"};
    private static final double ESCALA = 2;
    /** Mensaje de ejemplo para la captura del diálogo de error de conexión. */
    private static final String MENSAJE_SIN_SERVIDOR = "No se pudo conectar con el servidor MySQL. "
            + "Verifique que el servicio esté en ejecución y que la dirección de db.properties sea correcta.";

    private Capturador() {
    }

    /**
     * Genera las capturas.
     *
     * @param args carpeta de destino (opcional)
     * @throws Exception si falla alguna captura
     */
    public static void main(String[] args) throws Exception {
        System.setProperty(App.PROPIEDAD_DATOS, "memoria");
        if (System.getProperty("sgsld.hoy") == null) {
            System.setProperty("sgsld.hoy", "2026-09-26");
        }
        Path destino = args.length > 0 ? Path.of(args[0]) : Path.of("target", "capturas");
        Files.createDirectories(destino);
        Locale.setDefault(Texto.LOCALE);
        Lanzador.silenciarAvisoDeConfiguracion();
        CompletableFuture<List<Path>> resultado = new CompletableFuture<>();
        Platform.startup(() -> {
            try {
                resultado.complete(capturar(destino));
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

    /** Recorre las pantallas y guarda una imagen de cada una. */
    private static List<Path> capturar(Path destino) throws IOException, SgsldException {
        List<String> avisos = new ArrayList<>();
        Clock reloj = App.crearReloj(avisos);
        App.ModoDatos modo = App.modoDatos(avisos);
        Stage ventana = new Stage();
        ContextoFX contexto = new ContextoFX(ventana, reloj, modo, avisos, Runnable::run);
        contexto.conectar();
        List<Path> guardadas = new ArrayList<>();

        contexto.mostrarLogin();
        ventana.show();
        esperarTareas(contexto);
        guardadas.add(guardar(ventana.getScene().getRoot(), destino.resolve("01-login.png")));

        int numero = 2;
        for (String usuario : USUARIOS) {
            contexto.getServicios().autenticacion().iniciarSesion(usuario, CONTRASENA_LOTE.toCharArray());
            contexto.mostrarPrincipal();
            PrincipalController principal = (PrincipalController) contexto.getPantallaActual().controlador();
            for (OpcionMenu opcion : principal.opcionesHabilitadas()) {
                principal.abrir(opcion);
                esperarTareas(contexto);
                String nombre = String.format("%02d-%s-%s.png", numero++, usuario,
                        opcion.archivoFxml().replace(".fxml", "").replace('_', '-'));
                guardadas.add(guardar(ventana.getScene().getRoot(), destino.resolve(nombre)));
            }
            contexto.getServicios().autenticacion().cerrarSesion();
        }

        Alert dialogo = DialogoErrorConexion.crear(ventana, MENSAJE_SIN_SERVIDOR);
        dialogo.show();
        guardadas.add(guardar(dialogo.getDialogPane(), destino.resolve(String.format("%02d-error-conexion.png", numero))));
        dialogo.close();
        ventana.close();
        return guardadas;
    }

    /**
     * Procesa los eventos pendientes del hilo de JavaFX (entre ellos, los
     * resultados de las tareas) hasta que no quede ninguna tarea en curso.
     */
    private static void esperarTareas(ContextoFX contexto) {
        do {
            Object clave = new Object();
            Platform.runLater(() -> Platform.exitNestedEventLoop(clave, null));
            Platform.enterNestedEventLoop(clave);
        } while (contexto.hayTareasEnCurso());
    }

    /** Guarda la imagen del nodo en PNG, al doble de resolución. */
    private static Path guardar(Node nodo, Path archivo) throws IOException {
        SnapshotParameters parametros = new SnapshotParameters();
        parametros.setTransform(Transform.scale(ESCALA, ESCALA));
        WritableImage imagen = nodo.snapshot(parametros, null);
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        int[] pixeles = new int[ancho * alto];
        imagen.getPixelReader().getPixels(0, 0, ancho, alto, PixelFormat.getIntArgbInstance(), pixeles, 0, ancho);
        BufferedImage salida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        salida.setRGB(0, 0, ancho, alto, pixeles, 0, ancho);
        ImageIO.write(salida, "png", archivo.toFile());
        return archivo;
    }
}
