package sgsld.vista.fx;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;

import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;
import sgsld.util.Fechas;

/**
 * Alerta de posible lesión recurrente (CU-11, Figura 11): cabecera naranja,
 * la zona y las fechas del antecedente en negrita, la distancia con la nueva
 * lesión y la aclaración de que es informativa. Tiene un solo botón,
 * "Entendido, continuar": la alerta no bloquea el registro.
 */
final class AlertaRecurrencia {

    static final ButtonType CONTINUAR = new ButtonType("Entendido, continuar", ButtonData.OK_DONE);

    private AlertaRecurrencia() {
    }

    /**
     * Arma la alerta.
     *
     * @param duenio      ventana dueña
     * @param antecedente lesión anterior que activó la alerta
     * @param fechaNueva  fecha de la lesión que se está registrando
     * @return el diálogo, sin mostrar
     */
    static Dialog<ButtonType> crear(Window duenio, Lesion antecedente, LocalDate fechaNueva) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (duenio != null) {
            dialogo.initOwner(duenio);
        }
        dialogo.setTitle("SGSLD — Alerta de recurrencia");
        DialogPane panel = dialogo.getDialogPane();
        panel.getStylesheets().add(ContextoFX.hojaDeEstilo());
        panel.getStyleClass().add("alerta-recurrencia");

        Label titulo = new Label("⚠ Alerta: posible lesión recurrente");
        titulo.getStyleClass().add("alerta-titulo");
        HBox cabecera = new HBox(titulo);
        cabecera.getStyleClass().add("alerta-cabecera");
        panel.setHeader(cabecera);

        TextFlow texto = new TextFlow();
        texto.getChildren().addAll(partes(antecedente, fechaNueva));
        Label aclaracion = new Label("La alerta es informativa y no constituye un diagnóstico médico. "
                + "Puede continuar con el registro.");
        aclaracion.getStyleClass().add("nota");
        aclaracion.setWrapText(true);
        VBox contenido = new VBox(texto, aclaracion);
        contenido.getStyleClass().add("alerta-contenido");
        panel.setContent(contenido);
        panel.getButtonTypes().add(CONTINUAR);
        panel.setPrefWidth(600);
        return dialogo;
    }

    /**
     * Texto de la alerta con las partes destacadas, según el último
     * tratamiento del antecedente: sin tratamiento, en curso o finalizado
     * (con la distancia entre el fin y la nueva lesión).
     */
    private static List<Text> partes(Lesion antecedente, LocalDate fechaNueva) {
        List<Text> partes = new ArrayList<>();
        partes.add(texto("El jugador registra una lesión anterior en la misma zona corporal:\n", false));
        partes.add(texto(antecedente.getZona().getNombre() + " · " + Fechas.formatear(antecedente.getFecha()), true));
        Tratamiento ultimo = antecedente.getUltimoTratamiento();
        if (ultimo == null) {
            partes.add(texto(", sin tratamiento registrado.", false));
        } else if (ultimo.estaEnCurso()) {
            partes.add(texto(", con el tratamiento en curso.", false));
        } else {
            LocalDate fin = ultimo.getFechaFin();
            partes.add(texto(", tratamiento finalizado el ", false));
            partes.add(texto(Fechas.formatear(fin), true));
            partes.add(texto(fin.isAfter(fechaNueva)
                    ? "\n(" + Fechas.describirPeriodo(fechaNueva, fin) + " después de la nueva lesión)."
                    : "\n(" + Fechas.describirPeriodo(fin, fechaNueva) + " antes de la nueva lesión).", false));
        }
        return partes;
    }

    private static Text texto(String contenido, boolean destacado) {
        Text texto = new Text(contenido);
        texto.getStyleClass().add(destacado ? "alerta-destacado" : "alerta-texto");
        return texto;
    }
}
