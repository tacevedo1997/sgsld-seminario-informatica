package sgsld.vista.fx;

import java.util.logging.Logger;

import javafx.application.Application;

import sgsld.App;

/**
 * Punto de entrada del JAR ejecutable.
 * <p>
 * No extiende {@link Application}: si la clase principal la extendiera, el
 * lanzador de Java exigiría JavaFX en el <i>module path</i> y el JAR con las
 * dependencias adentro no arrancaría. Con {@code --consola} inicia la vista de
 * consola de la entrega 3 ({@link App#main(String[])}); sin opciones, la
 * interfaz JavaFX ({@link AplicacionFX}). Las dos vistas usan el mismo modelo
 * y los mismos servicios.
 */
public final class Lanzador {

    /** Opción que inicia la vista de consola. */
    public static final String OPCION_CONSOLA = "--consola";

    /** Comienzo del aviso que JavaFX emite cuando se carga desde el classpath. */
    private static final String AVISO_CONFIGURACION = "Unsupported JavaFX configuration";

    /**
     * Registro "javafx" de {@code java.util.logging}. La referencia fuerte
     * evita que se libere y se pierda el filtro.
     */
    private static final Logger REGISTRO_JAVAFX = Logger.getLogger("javafx");

    private Lanzador() {
    }

    /**
     * Inicia la interfaz JavaFX o, con {@code --consola}, la consola.
     *
     * @param args {@code --consola} para la vista de consola
     */
    public static void main(String[] args) {
        for (String argumento : args) {
            if (OPCION_CONSOLA.equals(argumento)) {
                App.main(args);
                return;
            }
        }
        silenciarAvisoDeConfiguracion();
        Application.launch(AplicacionFX.class, args);
    }

    /**
     * Filtra solo el aviso "Unsupported JavaFX configuration: classes were
     * loaded from 'unnamed module'". JavaFX lo emite porque, dentro de un JAR
     * ejecutable, sus clases se cargan desde el classpath y no como módulos;
     * esa forma de ejecución funciona igual. El filtro se aplica únicamente al
     * registro "javafx": los avisos de CSS, diseño y demás siguen visibles.
     */
    static void silenciarAvisoDeConfiguracion() {
        REGISTRO_JAVAFX.setFilter(registro -> registro.getMessage() == null
                || !registro.getMessage().startsWith(AVISO_CONFIGURACION));
    }
}
