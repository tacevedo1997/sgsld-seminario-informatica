package sgsld.dao.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sgsld.util.PersistenciaException;
import sgsld.util.PersistenciaException.Motivo;

/** Lectura y validación de {@code db.properties}, sin MySQL. */
@DisplayName("ConfiguracionBD · lectura de db.properties")
class ConfiguracionBDTest {

    private static final String URL = "jdbc:mysql://localhost:3306/sgsld?sslMode=REQUIRED";

    @TempDir
    Path directorio;

    private Path escribir(String contenido) throws Exception {
        Path archivo = directorio.resolve("db.properties");
        Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
        return archivo;
    }

    @Test
    @DisplayName("Archivo completo: lee URL, usuario y clave (en UTF-8) y obtiene el nombre de la base")
    void archivoCompleto() throws Exception {
        ConfiguracionBD configuracion = ConfiguracionBD.leer(
                escribir("db.url=" + URL + "\ndb.usuario=sgsld_app\ndb.clave=Contraseña-ñ\n"));
        assertEquals(URL, configuracion.getUrl());
        assertEquals("sgsld_app", configuracion.getUsuario());
        assertEquals("Contraseña-ñ", configuracion.getClave());
        assertEquals("sgsld", configuracion.getNombreBase());
    }

    @Test
    @DisplayName("La clave nunca aparece en toString()")
    void claveOculta() throws Exception {
        ConfiguracionBD configuracion = ConfiguracionBD.leer(
                escribir("db.url=" + URL + "\ndb.usuario=sgsld_app\ndb.clave=secreta123\n"));
        assertFalse(configuracion.toString().contains("secreta123"));
    }

    @Test
    @DisplayName("La clave puede quedar vacía (como en el .ejemplo)")
    void claveVacia() throws Exception {
        assertEquals("", ConfiguracionBD.leer(escribir("db.url=" + URL + "\ndb.usuario=sgsld_app\ndb.clave=\n"))
                .getClave());
    }

    @Test
    @DisplayName("Archivo incompleto: informa las claves que faltan")
    void archivoIncompleto() throws Exception {
        Path archivo = escribir("db.url=" + URL + "\n");
        PersistenciaException error = assertThrows(PersistenciaException.class, () -> ConfiguracionBD.leer(archivo));
        assertEquals(Motivo.CONFIGURACION, error.getMotivo());
        assertEquals("El archivo db.properties no tiene las claves obligatorias: db.usuario, db.clave.",
                error.getMessage());
    }

    @Test
    @DisplayName("Archivo inexistente: el mensaje nombra el archivo y el .ejemplo")
    void archivoInexistente() {
        Path archivo = directorio.resolve("db.properties");
        PersistenciaException error = assertThrows(PersistenciaException.class, () -> ConfiguracionBD.leer(archivo));
        assertEquals(Motivo.CONFIGURACION, error.getMotivo());
        assertTrue(error.getMessage().contains("db.properties"));
        assertTrue(error.getMessage().contains("db.properties.ejemplo"));
    }

    @Test
    @DisplayName("URL que no es de MySQL: rechazo")
    void urlInvalida() throws Exception {
        Path archivo = escribir("db.url=jdbc:postgresql://localhost/sgsld\ndb.usuario=sgsld_app\ndb.clave=x\n");
        PersistenciaException error = assertThrows(PersistenciaException.class, () -> ConfiguracionBD.leer(archivo));
        assertTrue(error.getMessage().contains("jdbc:mysql://"));
    }
}
