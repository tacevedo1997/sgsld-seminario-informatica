package sgsld.dao.mysql;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import sgsld.util.PersistenciaException;

/**
 * Base de las pruebas con MySQL: {@code sgsld_test}, nunca {@code sgsld}.
 * <p>
 * {@link #recrear()} la elimina y la vuelve a crear con las secciones 1 a 3 de
 * {@code sgsld_script.sql} (el nombre de la base se adapta), de modo que cada
 * clase de prueba parte del lote original. Las credenciales se leen de
 * {@code db-test.properties} (no versionado).
 */
public final class BaseDePruebasMySQL {

    /** Archivo de configuración de las pruebas, en la raíz del proyecto. */
    public static final Path ARCHIVO = Path.of("db-test.properties");

    private static final String BASE = "sgsld_test";

    private BaseDePruebasMySQL() {
    }

    /**
     * Lee {@code db-test.properties} y controla que apunte a {@code sgsld_test}.
     *
     * @return la configuración de pruebas
     * @throws PersistenciaException si el archivo es inválido
     */
    public static ConfiguracionBD configuracion() throws PersistenciaException {
        if (!Files.isRegularFile(ARCHIVO)) {
            throw new IllegalStateException("Falta db-test.properties en la raíz del proyecto: créelo a partir de "
                    + "db-test.properties.ejemplo (ver README).");
        }
        ConfiguracionBD configuracion = ConfiguracionBD.leer(ARCHIVO);
        if (!BASE.equals(configuracion.getNombreBase())) {
            throw new IllegalStateException("Por seguridad, las pruebas solo usan la base " + BASE
                    + "; db-test.properties apunta a '" + configuracion.getNombreBase() + "'.");
        }
        return configuracion;
    }

    /**
     * Elimina y vuelve a crear {@code sgsld_test} con el lote de prueba.
     *
     * @return una conexión configurada para la base recién creada
     * @throws Exception si falla la configuración o alguna sentencia
     */
    public static ConexionBD recrear() throws Exception {
        ConfiguracionBD configuracion = configuracion();
        String url = configuracion.getUrl() + (configuracion.getUrl().contains("?") ? "&" : "?")
                + "createDatabaseIfNotExist=true";
        try (Connection conexion = DriverManager.getConnection(url, configuracion.getUsuario(),
                configuracion.getClave());
             Statement sentencia = conexion.createStatement()) {
            for (String sql : sentencias()) {
                sentencia.execute(sql);
            }
        }
        return ConexionBD.crear(configuracion);
    }

    /**
     * Sentencias de las secciones 1 a 3 del script, sin comentarios y con la
     * base renombrada a {@code sgsld_test}.
     *
     * @return las sentencias, en orden
     * @throws IOException si no se puede leer el script
     */
    static List<String> sentencias() throws IOException {
        StringBuilder sinComentarios = new StringBuilder();
        for (String linea : ScriptBase.seccionesUnoATres(ScriptBase.leer(ScriptBase.SCRIPT)).split("\n")) {
            if (!linea.trim().startsWith("--")) {
                sinComentarios.append(linea).append('\n');
            }
        }
        List<String> sentencias = new ArrayList<>();
        int renombradas = 0;
        for (String parte : sinComentarios.toString().split(";\\s*\n")) {
            String sql = parte.trim();
            if (sql.isEmpty()) {
                continue;
            }
            String adaptada = sql.replaceFirst("^DROP DATABASE IF EXISTS sgsld$", "DROP DATABASE IF EXISTS " + BASE)
                    .replaceFirst("^CREATE DATABASE sgsld ", "CREATE DATABASE " + BASE + " ")
                    .replaceFirst("^USE sgsld$", "USE " + BASE);
            if (!adaptada.equals(sql)) {
                renombradas++;
            }
            sentencias.add(adaptada);
        }
        if (renombradas != 3) {
            throw new IllegalStateException("No se pudo adaptar el nombre de la base en el script");
        }
        return sentencias;
    }
}
