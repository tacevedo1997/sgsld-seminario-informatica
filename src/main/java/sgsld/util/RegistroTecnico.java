package sgsld.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Log técnico del sistema con {@code java.util.logging}.
 * <p>
 * Los detalles que nunca se muestran al usuario (SQLState, código de error,
 * trazas) se escriben en {@code logs/sgsld.log}. El registro no escribe en la
 * consola: hasta que se llama a {@link #configurar(Path)}, como ocurre en las
 * pruebas, los mensajes se descartan.
 */
public final class RegistroTecnico {

    /** Nombre del logger raíz del sistema. */
    public static final String NOMBRE = "sgsld";

    /** Referencia fuerte: {@code java.util.logging} solo guarda referencias débiles. */
    private static final Logger LOGGER = Logger.getLogger(NOMBRE);

    private static boolean configurado;

    static {
        LOGGER.setUseParentHandlers(false);
    }

    private RegistroTecnico() {
    }

    /**
     * Devuelve el logger del sistema.
     *
     * @return el logger {@value #NOMBRE}
     */
    public static Logger logger() {
        return LOGGER;
    }

    /**
     * Agrega la salida a {@code <directorio>/sgsld.log}. Se llama una sola vez
     * al iniciar la aplicación.
     *
     * @param directorio directorio de los logs (por ejemplo, {@code logs})
     * @return {@code true} si el archivo quedó configurado; {@code false} si no
     *         se pudo crear (la aplicación sigue funcionando sin log)
     */
    public static synchronized boolean configurar(Path directorio) {
        if (configurado) {
            return true;
        }
        try {
            Files.createDirectories(directorio);
            FileHandler archivo = new FileHandler(directorio.resolve("sgsld.log").toString(), true);
            archivo.setEncoding("UTF-8");
            archivo.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(archivo);
            LOGGER.setLevel(Level.INFO);
            configurado = true;
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }
}
