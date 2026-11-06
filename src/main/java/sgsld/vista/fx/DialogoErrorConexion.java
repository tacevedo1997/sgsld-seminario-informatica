package sgsld.vista.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/**
 * Diálogo que se muestra cuando no se puede conectar con MySQL al iniciar:
 * explica el motivo (sin detalles técnicos, que quedan en el registro) y
 * ofrece Reintentar o Salir.
 */
final class DialogoErrorConexion {

    static final ButtonType REINTENTAR = new ButtonType("Reintentar", ButtonData.OK_DONE);
    static final ButtonType SALIR = new ButtonType("Salir", ButtonData.CANCEL_CLOSE);

    private DialogoErrorConexion() {
    }

    /**
     * Crea el diálogo sin mostrarlo.
     *
     * @param duenio  ventana dueña
     * @param mensaje motivo, tal como lo informa la {@code PersistenciaException}
     * @return el diálogo
     */
    static Alert crear(Window duenio, String mensaje) {
        return Dialogos.crear(duenio, AlertType.ERROR, "Error de conexión",
                "No se pudo conectar con la base de datos",
                mensaje + "\n\nPuede reintentar la conexión o salir de la aplicación.", REINTENTAR, SALIR);
    }
}
