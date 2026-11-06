package sgsld.vista.fx;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import sgsld.modelo.Entidad;
import sgsld.modelo.Jugador;
import sgsld.util.Fechas;
import sgsld.util.Texto;

/**
 * Ayudas para los campos de los formularios: fechas con formato
 * {@code dd/MM/aaaa} estricto y campos que solo admiten dígitos.
 * <p>
 * Los errores de formato que detecta la vista (una fecha mal escrita, un
 * número faltante) se informan juntos antes de llamar al servicio; las reglas
 * de negocio las valida el servicio, que también informa todos sus errores
 * juntos.
 */
final class Campos {

    /** Texto de ayuda de los campos de fecha. */
    static final String FORMATO_FECHA = "dd/mm/aaaa";

    private Campos() {
    }

    /**
     * Configura un selector de fecha con el formato {@code dd/MM/aaaa} y sin
     * días posteriores a la fecha máxima en el calendario.
     *
     * @param selector selector de fecha
     * @param maxima   última fecha elegible (normalmente, hoy)
     */
    static void configurarFecha(DatePicker selector, LocalDate maxima) {
        selector.setPromptText(FORMATO_FECHA);
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate fecha) {
                return fecha == null ? "" : Fechas.formatear(fecha);
            }

            @Override
            public LocalDate fromString(String texto) {
                return interpretar(texto);
            }
        });
        selector.setDayCellFactory(calendario -> new DateCell() {
            @Override
            public void updateItem(LocalDate fecha, boolean vacia) {
                super.updateItem(fecha, vacia);
                setDisable(vacia || fecha.isAfter(maxima));
            }
        });
    }

    /**
     * Lee la fecha escrita en el selector (aunque el usuario no haya salido
     * del campo).
     *
     * @param selector    selector de fecha
     * @param descripcion nombre del campo para el mensaje, por ejemplo "fecha de nacimiento"
     * @param errores     lista donde se agrega el error de formato
     * @return la fecha, o {@code null} si está vacía o mal escrita
     */
    static LocalDate leerFecha(DatePicker selector, String descripcion, List<String> errores) {
        String texto = selector.getEditor().getText();
        if (Texto.estaVacio(texto)) {
            return null;
        }
        LocalDate fecha = interpretar(texto);
        if (fecha == null) {
            errores.add("La " + descripcion + " debe tener el formato " + FORMATO_FECHA + " (por ejemplo, 26/09/2026).");
        }
        return fecha;
    }

    /**
     * Muestra una fecha en el selector.
     *
     * @param selector selector de fecha
     * @param fecha    fecha a mostrar (puede ser {@code null})
     */
    static void mostrarFecha(DatePicker selector, LocalDate fecha) {
        selector.setValue(fecha);
        selector.getEditor().setText(fecha == null ? "" : Fechas.formatear(fecha));
    }

    /**
     * Hace que el campo solo acepte dígitos, hasta la cantidad indicada.
     *
     * @param campo      campo de texto
     * @param maxDigitos cantidad máxima de dígitos
     */
    static void soloDigitos(TextField campo, int maxDigitos) {
        campo.setTextFormatter(new TextFormatter<String>(cambio -> {
            String nuevo = cambio.getControlNewText();
            return nuevo.length() <= maxDigitos && nuevo.chars().allMatch(Character::isDigit) ? cambio : null;
        }));
    }

    /**
     * Lee un número entero de un campo que solo admite dígitos.
     *
     * @param campo campo de texto
     * @return el número, o {@code null} si está vacío
     */
    static Integer leerEntero(TextField campo) {
        String texto = campo.getText();
        return Texto.estaVacio(texto) ? null : Integer.valueOf(texto.trim());
    }

    /**
     * Texto del campo sin espacios al principio ni al final.
     *
     * @param campo campo de texto
     * @return el texto, o {@code ""} si está vacío
     */
    static String leerTexto(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    /**
     * Define cómo se muestra cada elemento de un combo, en la lista y en el
     * botón (que, vacío, muestra el texto de ayuda).
     *
     * @param combo combo
     * @param texto texto de cada elemento
     * @param <T>   tipo de los elementos
     */
    static <T> void mostrarCon(ComboBox<T> combo, Function<T, String> texto) {
        combo.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(T elemento, boolean vacia) {
                super.updateItem(elemento, vacia);
                setText(vacia || elemento == null ? null : texto.apply(elemento));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T elemento, boolean vacia) {
                super.updateItem(elemento, vacia);
                setText(vacia || elemento == null ? combo.getPromptText() : texto.apply(elemento));
            }
        });
    }

    /**
     * Elige en el combo el elemento con ese número (tras releer, los objetos
     * pueden ser otros).
     *
     * @param combo combo
     * @param id    número buscado
     * @param <T>   tipo de los elementos
     * @return {@code true} si lo encontró
     */
    static <T extends Entidad> boolean elegirPorId(ComboBox<T> combo, int id) {
        for (T elemento : combo.getItems()) {
            if (elemento.getId() == id) {
                combo.setValue(elemento);
                return true;
            }
        }
        return false;
    }

    /**
     * Texto de un jugador en los combos, como en la Figura 11.
     *
     * @param jugador jugador
     * @return por ejemplo "Gómez, Pedro — DNI 42587634 — Primera"
     */
    static String describirJugador(Jugador jugador) {
        return jugador.getNombreCompleto() + " — DNI " + jugador.getDni() + " — " + jugador.getCategoria().getNombre();
    }

    private static LocalDate interpretar(String texto) {
        if (Texto.estaVacio(texto)) {
            return null;
        }
        try {
            return Fechas.parsear(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
