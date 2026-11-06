package sgsld.dao.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.util.PersistenciaException;
import sgsld.util.PersistenciaException.Motivo;

/** Traducción de {@code SQLException} a {@code PersistenciaException}, sin MySQL. */
@DisplayName("TraductorErrores · SQLState y código de MySQL a mensajes claros")
class TraductorErroresTest {

    private static PersistenciaException traducir(String mensaje, String estado, int codigo) {
        return TraductorErrores.traducir(new SQLException(mensaje, estado, codigo), "registrar el jugador");
    }

    @Test
    @DisplayName("1062 · clave duplicada, con el índice violado y sin el texto de MySQL")
    void claveDuplicada() {
        SQLException original = new SQLException(
                "Duplicate entry '42587634' for key 'jugador.uq_jugador_dni'", "23000", 1062);
        PersistenciaException error = TraductorErrores.traducir(original, "registrar el jugador");
        assertEquals(Motivo.CLAVE_DUPLICADA, error.getMotivo());
        assertTrue(error.esClaveDuplicada());
        assertEquals("uq_jugador_dni", error.getRestriccion());
        assertFalse(error.getMessage().contains("Duplicate"));
        assertSame(original, error.getCause());
    }

    @Test
    @DisplayName("1045 · acceso denegado: revisar usuario y clave de db.properties")
    void accesoDenegado() {
        PersistenciaException error = traducir("Access denied for user 'sgsld_app'@'localhost'", "28000", 1045);
        assertEquals(Motivo.ACCESO_DENEGADO, error.getMotivo());
        assertTrue(error.getMessage().contains("db.properties"));
    }

    @Test
    @DisplayName("1142 · comando no permitido: el usuario no tiene permiso")
    void comandoNoPermitido() {
        PersistenciaException error = traducir("DELETE command denied to user", "42000", 1142);
        assertEquals(Motivo.ACCESO_DENEGADO, error.getMotivo());
        assertTrue(error.getMessage().contains("no tiene permiso para registrar el jugador"));
    }

    @Test
    @DisplayName("1049 · base inexistente: crearla con sql/crear_base.sql")
    void baseInexistente() {
        PersistenciaException error = traducir("Unknown database 'sgsld'", "42000", 1049);
        assertEquals(Motivo.BASE_INEXISTENTE, error.getMotivo());
        assertTrue(error.getMessage().contains("sql/crear_base.sql"));
    }

    @Test
    @DisplayName("08S01 · servidor apagado o inaccesible")
    void sinConexion() {
        PersistenciaException error = traducir("Communications link failure", "08S01", 0);
        assertEquals(Motivo.CONEXION, error.getMotivo());
        assertTrue(error.getMessage().contains("servicio esté en ejecución"));
    }

    @Test
    @DisplayName("Falla de TLS en la cadena de causas")
    void sinTls() {
        SQLException error = new SQLException("Communications link failure", "08S01", 0,
                new SSLHandshakeException("handshake failed"));
        assertEquals(Motivo.TLS, TraductorErrores.traducir(error, "conectar con la base de datos").getMotivo());
    }

    @Test
    @DisplayName("Tiempo de espera agotado")
    void tiempoAgotado() {
        PersistenciaException error = TraductorErrores.traducir(
                new SQLTimeoutException("Statement cancelled due to timeout"), "consultar las lesiones");
        assertEquals(Motivo.TIEMPO_AGOTADO, error.getMotivo());
    }

    @Test
    @DisplayName("3819 · restricción CHECK violada, con su nombre")
    void restriccionCheck() {
        PersistenciaException error = traducir("Check constraint 'ck_tratamiento_fechas' is violated.", "HY000", 3819);
        assertEquals(Motivo.RESTRICCION, error.getMotivo());
        assertEquals("ck_tratamiento_fechas", error.getRestriccion());
    }

    @Test
    @DisplayName("1452 · clave foránea violada, con su nombre")
    void claveForanea() {
        PersistenciaException error = traducir("Cannot add or update a child row: a foreign key constraint fails "
                + "(`sgsld`.`lesion`, CONSTRAINT `fk_lesion_jugador` FOREIGN KEY (`id_jugador`))", "23000", 1452);
        assertEquals(Motivo.REFERENCIA, error.getMotivo());
        assertEquals("fk_lesion_jugador", error.getRestriccion());
    }

    @Test
    @DisplayName("Cualquier otro error: mensaje genérico con la operación, sin detalles técnicos")
    void otroError() {
        PersistenciaException error = traducir("You have an error in your SQL syntax", "42000", 1064);
        assertEquals(Motivo.OTRO, error.getMotivo());
        assertEquals("No se pudo registrar el jugador por un error de la base de datos. "
                + "El detalle quedó en el registro técnico.", error.getMessage());
    }
}
