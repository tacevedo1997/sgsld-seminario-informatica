package sgsld.vista.fx;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Task;
import javafx.stage.Window;

import sgsld.modelo.Usuario;
import sgsld.servicio.Servicios;
import sgsld.util.RegistroTecnico;
import sgsld.util.SesionUsuario;
import sgsld.util.SgsldException;
import sgsld.util.ValidacionException;

/**
 * Base de los controladores JavaFX (uno por pantalla FXML).
 * <p>
 * Da acceso a los servicios, la sesión y la fecha actual; muestra errores,
 * avisos y confirmaciones; y ejecuta las operaciones contra los datos con un
 * {@link Task} fuera del hilo de JavaFX ({@link #ejecutar(Tarea, Consumer)}),
 * de modo que la ventana no se congela. El resultado y los errores vuelven al
 * hilo de JavaFX: los de validación se muestran juntos, los de persistencia
 * con su mensaje para el usuario y los inesperados con un texto genérico (la
 * traza queda en el registro técnico, nunca en pantalla).
 */
public abstract class ControladorPantalla {

    /** Mensaje para errores no previstos. */
    static final String MENSAJE_INESPERADO =
            "Ocurrió un error inesperado. El detalle quedó en el registro técnico (logs/sgsld.log).";

    private final ReadOnlyBooleanWrapper ocupado = new ReadOnlyBooleanWrapper(false);
    private ContextoFX contexto;
    private int tareasEnCurso;

    /**
     * Operación que se ejecuta fuera del hilo de JavaFX, normalmente una
     * llamada a un servicio.
     *
     * @param <T> tipo del resultado ({@code Void} si no devuelve nada)
     */
    @FunctionalInterface
    protected interface Tarea<T> {
        /**
         * Ejecuta la operación.
         *
         * @return el resultado
         * @throws SgsldException si el servicio la rechaza o falla la persistencia
         */
        T ejecutar() throws SgsldException;
    }

    /** Lo llama {@link ContextoFX} al crear el controlador, antes de cargar el FXML. */
    final void asignarContexto(ContextoFX contexto) {
        this.contexto = contexto;
    }

    /**
     * Título de la pantalla; la ventana muestra "SGSLD — título".
     *
     * @return el título
     */
    public abstract String getTitulo();

    /**
     * Carga los datos al entrar en la pantalla. Por defecto no hace nada.
     */
    public void alMostrar() {
    }

    /**
     * Indica si hay una tarea en curso; las pantallas lo usan para deshabilitar
     * sus botones mientras tanto.
     *
     * @return la propiedad de solo lectura
     */
    public final ReadOnlyBooleanProperty ocupadoProperty() {
        return ocupado.getReadOnlyProperty();
    }

    protected final ContextoFX contexto() {
        return contexto;
    }

    protected final Servicios servicios() {
        return contexto.getServicios();
    }

    /**
     * Usuario de la sesión actual.
     *
     * @return el usuario, o {@code null} si no hay sesión
     */
    protected final Usuario usuario() {
        return SesionUsuario.getInstancia().getUsuario();
    }

    /**
     * Fecha actual según el reloj de la aplicación ({@code -Dsgsld.hoy}).
     *
     * @return la fecha de hoy
     */
    protected final LocalDate hoy() {
        return contexto.hoy();
    }

    protected final Window ventana() {
        return contexto.getVentana();
    }

    /**
     * Ejecuta la tarea fuera del hilo de JavaFX. Si termina bien, entrega el
     * resultado en el hilo de JavaFX; si falla, muestra el error.
     *
     * @param tarea      operación contra los servicios
     * @param alTerminar qué hacer con el resultado
     * @param <T>        tipo del resultado
     */
    protected final <T> void ejecutar(Tarea<T> tarea, Consumer<T> alTerminar) {
        ejecutar(tarea, alTerminar, this::informarError);
    }

    /**
     * Ejecuta la tarea fuera del hilo de JavaFX, con un manejo de error propio.
     *
     * @param tarea      operación contra los servicios
     * @param alTerminar qué hacer con el resultado
     * @param alFallar   qué hacer con el error
     * @param <T>        tipo del resultado
     */
    protected final <T> void ejecutar(Tarea<T> tarea, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> trabajo = new Task<>() {
            @Override
            protected T call() throws SgsldException {
                return tarea.ejecutar();
            }
        };
        trabajo.setOnSucceeded(evento -> {
            terminar();
            alTerminar.accept(trabajo.getValue());
        });
        trabajo.setOnFailed(evento -> {
            terminar();
            alFallar.accept(trabajo.getException());
        });
        empezar();
        contexto.getEjecutor().execute(trabajo);
    }

    /**
     * Muestra un error según su tipo: validación (todos los errores juntos),
     * error conocido (su mensaje) o inesperado (texto genérico y traza al
     * registro técnico).
     *
     * @param error error de la tarea
     */
    protected void informarError(Throwable error) {
        if (error instanceof ValidacionException validacion) {
            mostrarErrores(validacion.getErrores());
        } else if (error instanceof SgsldException conocido) {
            mostrarError(conocido.getMessage());
        } else {
            RegistroTecnico.logger().log(Level.SEVERE, "Error inesperado en " + getClass().getSimpleName(), error);
            mostrarError(MENSAJE_INESPERADO);
        }
    }

    protected final void mostrarError(String mensaje) {
        Dialogos.error(ventana(), mensaje);
    }

    protected final void mostrarErrores(List<String> errores) {
        Dialogos.errores(ventana(), errores);
    }

    protected final void mostrarInfo(String mensaje) {
        Dialogos.info(ventana(), mensaje);
    }

    /**
     * Pide confirmación con los botones Sí y No.
     *
     * @param pregunta texto de la pregunta
     * @return {@code true} si se eligió Sí
     */
    protected final boolean confirmar(String pregunta) {
        return Dialogos.confirmar(ventana(), pregunta);
    }

    private void empezar() {
        tareasEnCurso++;
        ocupado.set(true);
        contexto.tareaIniciada();
    }

    private void terminar() {
        tareasEnCurso--;
        ocupado.set(tareasEnCurso > 0);
        contexto.tareaTerminada();
    }
}
