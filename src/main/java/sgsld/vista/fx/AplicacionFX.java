package sgsld.vista.fx;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import sgsld.App;
import sgsld.util.RegistroTecnico;
import sgsld.util.SesionUsuario;
import sgsld.util.Texto;

/**
 * Aplicación JavaFX: la vista gráfica del SGSLD.
 * <p>
 * Usa la misma raíz de composición que la consola ({@link App}): reloj con
 * {@code -Dsgsld.hoy}, modo de datos con {@code -Dsgsld.datos}, registro
 * técnico y fábrica de DAO. La conexión se intenta al mostrar el inicio de
 * sesión, fuera del hilo de JavaFX; si falla, se ofrece reintentar o salir.
 */
public class AplicacionFX extends Application {

    private ExecutorService ejecutor;

    /** Idioma de los controles (calendario del {@code DatePicker}): español de Argentina. */
    @Override
    public void init() {
        Locale.setDefault(Texto.LOCALE);
    }

    /**
     * Arma el contexto y muestra el inicio de sesión, que conecta con la base.
     *
     * @param ventana ventana principal
     */
    @Override
    public void start(Stage ventana) {
        List<String> avisos = new ArrayList<>();
        Clock reloj = App.crearReloj(avisos);
        App.ModoDatos modo = App.modoDatos(avisos);
        App.configurarRegistro(avisos);
        ejecutor = Executors.newSingleThreadExecutor(tarea -> {
            Thread hilo = new Thread(tarea, "sgsld-datos");
            hilo.setDaemon(true);
            return hilo;
        });
        // Errores no previstos en el hilo de JavaFX: la traza va al registro técnico y el usuario
        // ve un mensaje genérico (con runLater, porque no se puede abrir un diálogo durante el diseño).
        Thread.currentThread().setUncaughtExceptionHandler((hilo, error) -> {
            RegistroTecnico.logger().log(Level.SEVERE, "Error inesperado en la interfaz", error);
            Platform.runLater(() -> Dialogos.error(ventana, ControladorPantalla.MENSAJE_INESPERADO));
        });
        ContextoFX contexto = new ContextoFX(ventana, reloj, modo, avisos, ejecutor);
        contexto.mostrarLogin();
        ventana.show();
    }

    /** Al cerrar la ventana: cierra la sesión y detiene el hilo de datos. */
    @Override
    public void stop() {
        SesionUsuario.getInstancia().cerrar();
        if (ejecutor != null) {
            ejecutor.shutdownNow();
        }
    }
}
