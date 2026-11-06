package sgsld.vista.fx;

import java.time.LocalDate;
import java.util.List;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import sgsld.modelo.Tratamiento;
import sgsld.util.Fechas;

/**
 * Confirmación de "Registrar y finalizar" (CU-06): propone como fecha de fin
 * la de la evolución, que el kinesiólogo puede cambiar. Si se cancela, no se
 * guarda nada; si se confirma, la evolución y la finalización se guardan en
 * una sola transacción.
 */
final class DialogoFinalizacion {

    static final ButtonType FINALIZAR = new ButtonType("Registrar y finalizar", ButtonData.YES);
    static final ButtonType CANCELAR = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
    private static final double ANCHO_TEXTO = Dialogos.ANCHO - 20;

    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final DatePicker campoFin = new DatePicker();

    /**
     * Arma el diálogo.
     *
     * @param duenio        ventana dueña
     * @param tratamiento   tratamiento en curso
     * @param finPropuesto  fecha de fin propuesta (la de la evolución)
     * @param hoy           fecha actual (no se admiten fechas futuras)
     */
    DialogoFinalizacion(Window duenio, Tratamiento tratamiento, LocalDate finPropuesto, LocalDate hoy) {
        if (duenio != null) {
            dialogo.initOwner(duenio);
        }
        dialogo.setTitle("SGSLD — Registrar y finalizar");
        DialogPane panel = dialogo.getDialogPane();
        panel.getStylesheets().add(ContextoFX.hojaDeEstilo());
        panel.setHeaderText("¿Registrar la evolución y finalizar el tratamiento N° " + tratamiento.getId() + "?");
        Label detalle = Dialogos.parrafo(tratamiento.getTipo() + " — "
                + tratamiento.getLesion().getJugador().getNombreCompleto() + "\nInicio: "
                + Fechas.formatear(tratamiento.getFechaInicio()) + ". Se propone como fin la fecha de la evolución.",
                ANCHO_TEXTO, null);
        Label etiquetaFin = new Label("Fecha de fin *");
        etiquetaFin.getStyleClass().add("etiqueta-campo");
        Campos.configurarFecha(campoFin, hoy);
        Campos.mostrarFecha(campoFin, finPropuesto);
        campoFin.setId("campoFin");
        Label nota = Dialogos.parrafo("La evolución y la finalización se guardan juntas, en una sola transacción: "
                + "si algo falla, no se guarda ninguna de las dos. Si cancela, el tratamiento sigue En curso.",
                ANCHO_TEXTO, "nota");
        VBox contenido = new VBox(detalle, new VBox(4, etiquetaFin, campoFin), nota);
        contenido.getStyleClass().add("dialogo-contenido");
        panel.setContent(contenido);
        panel.getButtonTypes().addAll(FINALIZAR, CANCELAR);
        panel.setPrefWidth(Dialogos.ANCHO + 40);
    }

    Dialog<ButtonType> dialogo() {
        return dialogo;
    }

    /**
     * Fecha de fin elegida.
     *
     * @param errores lista donde se agrega el error de formato
     * @return la fecha, o {@code null} si está vacía o mal escrita
     */
    LocalDate leerFechaFin(List<String> errores) {
        return Campos.leerFecha(campoFin, "fecha de fin", errores);
    }
}
