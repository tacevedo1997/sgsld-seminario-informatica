package sgsld.dao.mysql;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

import sgsld.util.PersistenciaException;
import sgsld.util.PersistenciaException.Motivo;
import sgsld.util.RegistroTecnico;

/**
 * Traduce una {@link SQLException} en una {@link PersistenciaException} con un
 * mensaje claro para el usuario, según el SQLState y el código de error de
 * MySQL. Los detalles técnicos (SQLState, código y traza) se escriben en el
 * log técnico; nunca llegan a la pantalla.
 */
public final class TraductorErrores {

    private static final Pattern CLAVE_UNICA = Pattern.compile("for key '([^']+)'");
    private static final Pattern RESTRICCION_CHECK = Pattern.compile("[Cc]onstraint '([^']+)'");
    private static final Pattern CLAVE_FORANEA = Pattern.compile("CONSTRAINT `([^`]+)`");

    private TraductorErrores() {
    }

    /**
     * Traduce el error y lo registra en el log técnico.
     *
     * @param error     excepción de JDBC
     * @param operacion qué se intentaba hacer, en infinitivo (por ejemplo,
     *                  "registrar el jugador"), para el mensaje y el log
     * @return la excepción traducida, con el error original como causa
     */
    public static PersistenciaException traducir(SQLException error, String operacion) {
        Motivo motivo = clasificar(error);
        String restriccion = restriccion(error);
        RegistroTecnico.logger().log(motivo == Motivo.CLAVE_DUPLICADA ? Level.WARNING : Level.SEVERE,
                String.format("Error de base de datos al %s: SQLState=%s, código=%d, motivo=%s",
                        operacion, error.getSQLState(), error.getErrorCode(), motivo), error);
        return new PersistenciaException(motivo, mensaje(motivo, error.getErrorCode(), operacion, restriccion),
                restriccion, error);
    }

    /**
     * Clasifica el error por su código de MySQL y, si no lo hay, por su SQLState.
     *
     * @param error excepción de JDBC
     * @return el motivo
     */
    static Motivo clasificar(SQLException error) {
        switch (error.getErrorCode()) {
            case 1044, 1045, 1142, 1143:
                return Motivo.ACCESO_DENEGADO;
            case 1049:
                return Motivo.BASE_INEXISTENTE;
            case 1062:
                return Motivo.CLAVE_DUPLICADA;
            case 1216, 1217, 1451, 1452:
                return Motivo.REFERENCIA;
            case 3819:
                return Motivo.RESTRICCION;
            case 1205, 3024:
                return Motivo.TIEMPO_AGOTADO;
            default:
                break;
        }
        String estado = error.getSQLState() == null ? "" : error.getSQLState();
        if (esProblemaDeTls(error)) {
            return Motivo.TLS;
        }
        if (error instanceof SQLTimeoutException || estado.startsWith("HYT")) {
            return Motivo.TIEMPO_AGOTADO;
        }
        if (estado.startsWith("08")) {
            return Motivo.CONEXION;
        }
        return Motivo.OTRO;
    }

    /** Busca en la cadena de causas una falla de la conexión segura. */
    private static boolean esProblemaDeTls(SQLException error) {
        Throwable actual = error;
        while (actual != null) {
            String mensaje = actual.getMessage() == null ? "" : actual.getMessage().toUpperCase(Locale.ROOT);
            if (actual instanceof SSLException || mensaje.contains("SSL") || mensaje.contains("TLS")) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    /**
     * Extrae el nombre del índice o la restricción violada del mensaje de MySQL.
     *
     * @param error excepción de JDBC
     * @return por ejemplo, {@code uq_jugador_dni}; {@code null} si no se identifica
     */
    static String restriccion(SQLException error) {
        String mensaje = error.getMessage() == null ? "" : error.getMessage();
        for (Pattern patron : new Pattern[] {CLAVE_UNICA, RESTRICCION_CHECK, CLAVE_FORANEA}) {
            Matcher coincidencia = patron.matcher(mensaje);
            if (coincidencia.find()) {
                String nombre = coincidencia.group(1);
                return nombre.substring(nombre.lastIndexOf('.') + 1);
            }
        }
        return null;
    }

    private static String mensaje(Motivo motivo, int codigo, String operacion, String restriccion) {
        String detalle = restriccion == null ? "" : " (" + restriccion + ")";
        switch (motivo) {
            case CONEXION:
                return "No se pudo conectar con el servidor MySQL. Verifique que el servicio esté en ejecución "
                        + "y que la dirección de db.properties sea correcta.";
            case ACCESO_DENEGADO:
                return codigo == 1045
                        ? "MySQL rechazó el acceso: revise el usuario y la clave de db.properties."
                        : "El usuario de la base de datos no tiene permiso para " + operacion + ".";
            case BASE_INEXISTENTE:
                return "La base de datos indicada en db.properties no existe. Créela con sql/crear_base.sql.";
            case TLS:
                return "No se pudo establecer una conexión segura (TLS) con el servidor MySQL. "
                        + "Verifique que el servidor tenga TLS habilitado.";
            case CLAVE_DUPLICADA:
                return "Ya existe un registro con esos datos" + detalle + ".";
            case REFERENCIA:
                return "La operación hace referencia a datos que no existen o que están en uso" + detalle + ".";
            case RESTRICCION:
                return "Los datos no cumplen una regla de la base de datos" + detalle + ".";
            case TIEMPO_AGOTADO:
                return "La base de datos no respondió a tiempo. Intente nuevamente.";
            default:
                return "Ocurrió un error de la base de datos al " + operacion + ". "
                        + "El detalle quedó en el registro técnico.";
        }
    }
}
