package sgsld.dao.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import sgsld.util.PersistenciaException;
import sgsld.util.PersistenciaException.Motivo;

/**
 * Conexión a MySQL con JDBC (patrón <b>Singleton</b> del diseño).
 * <p>
 * La aplicación usa una única instancia, obtenida con {@link #getInstancia()},
 * que lee y valida {@code db.properties} una sola vez. Cada operación pide una
 * conexión nueva con {@link #abrir()} y la cierra con try-with-resources. Las
 * pruebas crean una instancia independiente con {@link #crear(ConfiguracionBD)}
 * para trabajar sobre la base {@code sgsld_test}.
 */
public final class ConexionBD {

    /** Segundos de espera de {@link Connection#isValid(int)}. */
    private static final int SEGUNDOS_VALIDACION = 5;

    private static ConexionBD instancia;

    private final ConfiguracionBD configuracion;

    private ConexionBD(ConfiguracionBD configuracion) {
        this.configuracion = configuracion;
    }

    /**
     * Devuelve la instancia única de la aplicación. Si la configuración no se
     * pudo leer, el próximo llamado vuelve a intentarlo (por ejemplo, después
     * de corregir {@code db.properties} y elegir "Reintentar").
     *
     * @return la conexión configurada
     * @throws PersistenciaException si {@code db.properties} falta o es inválido
     */
    public static synchronized ConexionBD getInstancia() throws PersistenciaException {
        if (instancia == null) {
            instancia = new ConexionBD(ConfiguracionBD.leerPredeterminada());
        }
        return instancia;
    }

    /**
     * Crea una instancia independiente de la de la aplicación (pruebas).
     *
     * @param configuracion datos de conexión
     * @return la nueva instancia
     */
    public static ConexionBD crear(ConfiguracionBD configuracion) {
        return new ConexionBD(configuracion);
    }

    /**
     * Abre una conexión nueva. El llamador debe cerrarla (try-with-resources).
     *
     * @return la conexión abierta
     * @throws PersistenciaException si no se puede conectar, con el motivo en lenguaje claro
     */
    public Connection abrir() throws PersistenciaException {
        try {
            return DriverManager.getConnection(configuracion.getUrl(), configuracion.getUsuario(),
                    configuracion.getClave());
        } catch (SQLException e) {
            throw TraductorErrores.traducir(e, "conectar con la base de datos");
        }
    }

    /**
     * Comprueba que el servidor responda con los datos configurados.
     *
     * @throws PersistenciaException si no se puede conectar o la conexión no es válida
     */
    public void probarConexion() throws PersistenciaException {
        try (Connection conexion = abrir()) {
            if (!conexion.isValid(SEGUNDOS_VALIDACION)) {
                throw new PersistenciaException(Motivo.CONEXION,
                        "El servidor MySQL no respondió a tiempo. Intente nuevamente.", null);
            }
        } catch (SQLException e) {
            throw TraductorErrores.traducir(e, "probar la conexión");
        }
    }

    public ConfiguracionBD getConfiguracion() {
        return configuracion;
    }
}
