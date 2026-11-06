package sgsld.dao.mysql;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import sgsld.util.PersistenciaException;
import sgsld.util.PersistenciaException.Motivo;

/**
 * Datos de conexión a MySQL, leídos de un archivo {@code .properties} (sección
 * 15.7 del informe): {@code db.url}, {@code db.usuario} y {@code db.clave}.
 * <p>
 * La clave nunca se muestra: {@link #toString()} la oculta y los mensajes de
 * error solo nombran las claves faltantes.
 */
public final class ConfiguracionBD {

    /** Nombre del archivo de configuración de la aplicación. */
    public static final String ARCHIVO = "db.properties";
    /** Propiedad del sistema que indica otro archivo de configuración. */
    public static final String PROPIEDAD_RUTA = "sgsld.config";

    static final String CLAVE_URL = "db.url";
    static final String CLAVE_USUARIO = "db.usuario";
    static final String CLAVE_CLAVE = "db.clave";

    /** Claves que el archivo debe contener (arreglo de tamaño fijo y conocido). */
    private static final String[] CLAVES_OBLIGATORIAS = {CLAVE_URL, CLAVE_USUARIO, CLAVE_CLAVE};

    private final Path origen;
    private final String url;
    private final String usuario;
    private final String clave;

    private ConfiguracionBD(Path origen, String url, String usuario, String clave) {
        this.origen = origen;
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
    }

    /**
     * Lee la configuración de la aplicación: el archivo indicado con
     * {@code -Dsgsld.config} o, si no, {@value #ARCHIVO} ubicado con
     * {@link #ubicar(String)}.
     *
     * @return la configuración leída y validada
     * @throws PersistenciaException si el archivo no existe, no se puede leer o está incompleto
     */
    public static ConfiguracionBD leerPredeterminada() throws PersistenciaException {
        String ruta = System.getProperty(PROPIEDAD_RUTA);
        return leer(ruta == null || ruta.isBlank() ? ubicar(ARCHIVO) : Path.of(ruta));
    }

    /**
     * Busca un archivo de configuración junto al JAR en ejecución y, si no
     * está allí, en el directorio de trabajo.
     *
     * @param nombre nombre del archivo
     * @return la primera ruta existente, o la del directorio de trabajo si no
     *         existe en ninguna (para informarla en el error)
     */
    public static Path ubicar(String nombre) {
        Path enTrabajo = Path.of(System.getProperty("user.dir")).resolve(nombre);
        Path directorioJar = directorioDelJar();
        if (directorioJar != null && Files.isRegularFile(directorioJar.resolve(nombre))) {
            return directorioJar.resolve(nombre);
        }
        return enTrabajo;
    }

    /**
     * Lee y valida un archivo de configuración en UTF-8.
     *
     * @param archivo ruta del archivo
     * @return la configuración
     * @throws PersistenciaException si el archivo no existe, no se puede leer,
     *         le faltan claves o la URL no es de MySQL
     */
    public static ConfiguracionBD leer(Path archivo) throws PersistenciaException {
        Properties propiedades = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        } catch (NoSuchFileException e) {
            throw new PersistenciaException(Motivo.CONFIGURACION, "No se encontró el archivo de configuración "
                    + archivo.toAbsolutePath() + ". Créelo a partir de " + archivo.getFileName() + ".ejemplo.", e);
        } catch (IOException e) {
            throw new PersistenciaException(Motivo.CONFIGURACION, "No se pudo leer el archivo de configuración "
                    + archivo.toAbsolutePath() + ": " + e.getMessage(), e);
        }
        List<String> faltantes = new ArrayList<>();
        for (String clave : CLAVES_OBLIGATORIAS) {
            String valor = propiedades.getProperty(clave);
            boolean puedeQuedarVacia = clave.equals(CLAVE_CLAVE);
            if (valor == null || (!puedeQuedarVacia && valor.isBlank())) {
                faltantes.add(clave);
            }
        }
        if (!faltantes.isEmpty()) {
            throw new PersistenciaException(Motivo.CONFIGURACION, "El archivo " + archivo.getFileName()
                    + " no tiene las claves obligatorias: " + String.join(", ", faltantes) + ".", null);
        }
        String url = propiedades.getProperty(CLAVE_URL).trim();
        if (!url.startsWith("jdbc:mysql://")) {
            throw new PersistenciaException(Motivo.CONFIGURACION, "En " + archivo.getFileName()
                    + ", db.url debe comenzar con jdbc:mysql://", null);
        }
        return new ConfiguracionBD(archivo, url, propiedades.getProperty(CLAVE_USUARIO).trim(),
                propiedades.getProperty(CLAVE_CLAVE));
    }

    public Path getOrigen() {
        return origen;
    }

    public String getUrl() {
        return url;
    }

    public String getUsuario() {
        return usuario;
    }

    /** Clave del usuario: solo la usa {@link ConexionBD}. */
    String getClave() {
        return clave;
    }

    /**
     * Devuelve el nombre de la base de la URL (lo que sigue al host y puerto).
     *
     * @return por ejemplo, {@code sgsld}; cadena vacía si la URL no indica base
     */
    public String getNombreBase() {
        String resto = url.substring("jdbc:mysql://".length());
        int barra = resto.indexOf('/');
        if (barra < 0) {
            return "";
        }
        String base = resto.substring(barra + 1);
        int parametros = base.indexOf('?');
        return parametros < 0 ? base : base.substring(0, parametros);
    }

    /** Muestra la configuración sin la clave. */
    @Override
    public String toString() {
        return "ConfiguracionBD[url=" + url + ", usuario=" + usuario + ", clave=****]";
    }

    /** Directorio del JAR en ejecución, o {@code null} si se ejecuta desde clases sueltas. */
    private static Path directorioDelJar() {
        try {
            CodeSource fuente = ConfiguracionBD.class.getProtectionDomain().getCodeSource();
            if (fuente == null || fuente.getLocation() == null) {
                return null;
            }
            Path ubicacion = Path.of(fuente.getLocation().toURI());
            return Files.isRegularFile(ubicacion) ? ubicacion.getParent() : null;
        } catch (URISyntaxException | IllegalArgumentException | SecurityException e) {
            return null;
        }
    }
}
