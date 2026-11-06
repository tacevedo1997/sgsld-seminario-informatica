package sgsld.vista.fx;

import java.util.List;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Window;

/**
 * Diálogos comunes de la interfaz, con la hoja de estilo de la aplicación y
 * botones en español (no dependen del idioma del sistema). Esta clase solo los
 * arma; los muestra {@link ContextoFX#presentar(javafx.scene.control.Dialog)}.
 */
final class Dialogos {

    static final ButtonType ACEPTAR = new ButtonType("Aceptar", ButtonData.OK_DONE);
    static final ButtonType SI = new ButtonType("Sí", ButtonData.YES);
    static final ButtonType NO = new ButtonType("No", ButtonData.NO);

    private Dialogos() {
    }

    /**
     * Crea un diálogo modal sobre la ventana indicada.
     *
     * @param duenio     ventana dueña (puede ser {@code null})
     * @param tipo       tipo de alerta (define el ícono)
     * @param titulo     título de la ventana del diálogo, después de "SGSLD — "
     * @param encabezado texto destacado (o {@code null})
     * @param mensaje    texto del diálogo
     * @param botones    botones
     * @return el diálogo, sin mostrar
     */
    static Alert crear(Window duenio, AlertType tipo, String titulo, String encabezado, String mensaje,
                       ButtonType... botones) {
        Alert alerta = new Alert(tipo, mensaje, botones);
        if (duenio != null) {
            alerta.initOwner(duenio);
        }
        alerta.setTitle("SGSLD — " + titulo);
        alerta.setHeaderText(encabezado);
        alerta.getDialogPane().getStylesheets().add(ContextoFX.hojaDeEstilo());
        alerta.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return alerta;
    }

    static Alert crearError(Window duenio, String mensaje) {
        return crear(duenio, AlertType.ERROR, "Error", null, mensaje, ACEPTAR);
    }

    /**
     * Diálogo con todos los errores de validación juntos, uno por línea.
     *
     * @param duenio  ventana dueña
     * @param errores errores encontrados
     * @return el diálogo, sin mostrar
     */
    static Alert crearErrores(Window duenio, List<String> errores) {
        StringBuilder texto = new StringBuilder();
        for (String error : errores) {
            if (texto.length() > 0) {
                texto.append('\n');
            }
            texto.append("• ").append(error);
        }
        return crear(duenio, AlertType.WARNING, "Revise los datos", "Revise los datos ingresados:",
                texto.toString(), ACEPTAR);
    }

    static Alert crearInfo(Window duenio, String mensaje) {
        return crear(duenio, AlertType.INFORMATION, "Información", null, mensaje, ACEPTAR);
    }

    static Alert crearConfirmacion(Window duenio, String pregunta) {
        return crear(duenio, AlertType.CONFIRMATION, "Confirmar", null, pregunta, SI, NO);
    }

    /**
     * Muestra un error y espera, fuera de una pantalla (por ejemplo, si no se
     * pudo cargar un FXML o ante un error no previsto).
     *
     * @param duenio  ventana dueña
     * @param mensaje texto del error
     */
    static void error(Window duenio, String mensaje) {
        crearError(duenio, mensaje).showAndWait();
    }
}
