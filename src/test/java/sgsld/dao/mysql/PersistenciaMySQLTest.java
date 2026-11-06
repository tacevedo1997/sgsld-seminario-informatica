package sgsld.dao.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sgsld.EscenarioPrueba;
import sgsld.dao.JugadorDAO;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Rol;
import sgsld.modelo.Usuario;
import sgsld.servicio.JugadorService;
import sgsld.util.PersistenciaException;
import sgsld.util.PersistenciaException.Motivo;
import sgsld.util.SesionUsuario;
import sgsld.util.ValidacionException;

/** Pruebas que solo tienen sentido con MySQL: conexión, transacciones y concurrencia. */
@Tag("mysql")
@DisplayName("Persistencia MySQL · conexión, rollback y concurrencia (sgsld_test)")
class PersistenciaMySQLTest {

    private static ConexionBD conexion;

    @TempDir
    Path directorio;

    @BeforeAll
    static void recrearBase() throws Exception {
        conexion = BaseDePruebasMySQL.recrear();
    }

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    @Test
    @DisplayName("Conexión válida con db-test.properties (TLS requerido)")
    void conexionValida() throws Exception {
        conexion.probarConexion();
    }

    @Test
    @DisplayName("Clave incorrecta: ACCESO_DENEGADO con un mensaje claro y sin detalles técnicos")
    void claveIncorrecta() throws Exception {
        ConfiguracionBD valida = conexion.getConfiguracion();
        Path archivo = directorio.resolve("db.properties");
        Files.writeString(archivo, "db.url=" + valida.getUrl() + "\ndb.usuario=" + valida.getUsuario()
                + "\ndb.clave=clave-incorrecta\n", StandardCharsets.UTF_8);
        ConexionBD incorrecta = ConexionBD.crear(ConfiguracionBD.leer(archivo));
        PersistenciaException error = assertThrows(PersistenciaException.class, incorrecta::probarConexion);
        assertEquals(Motivo.ACCESO_DENEGADO, error.getMotivo());
        assertEquals("MySQL rechazó el acceso: revise el usuario y la clave de db.properties.", error.getMessage());
    }

    @Test
    @DisplayName("Servidor inaccesible (puerto sin MySQL): CONEXION con un mensaje claro")
    void servidorInaccesible() {
        ConfiguracionBD valida = conexion.getConfiguracion();
        ConexionBD sinServidor = ConexionBD.crear(valida.conUrl(valida.getUrl().replace(":3306/", ":3399/")));
        PersistenciaException error = assertThrows(PersistenciaException.class, sinServidor::probarConexion);
        assertEquals(Motivo.CONEXION, error.getMotivo());
        assertTrue(error.getMessage().startsWith("No se pudo conectar con el servidor MySQL"));
    }

    @Test
    @DisplayName("Rollback: si falla la segunda inserción (matrícula duplicada), el usuario no queda creado")
    void rollbackDeKinesiologoConUsuario() throws Exception {
        KinesiologoDAOMySQL kinesiologos = new KinesiologoDAOMySQL(conexion);
        Usuario usuario = new Usuario("usuario_rollback", "hash-de-prueba", Rol.KINESIOLOGO);
        PersistenciaException error = assertThrows(PersistenciaException.class, () -> kinesiologos
                .insertarConUsuario(new Kinesiologo("Ana", "Duplicada", "MP-10234", null), usuario));
        assertEquals(Motivo.CLAVE_DUPLICADA, error.getMotivo());
        assertEquals("uq_kinesiologo_matricula", error.getRestriccion());
        assertTrue(new UsuarioDAOMySQL(conexion).buscarPorNombreUsuario("usuario_rollback").isEmpty());
        assertEquals(0, usuario.getId());
    }

    @Test
    @DisplayName("Concurrencia: DNI que otro puesto registró después de la validación → mismo ValidacionException")
    void dniDuplicadoDetectadoPorLaBase() throws Exception {
        SesionUsuario.getInstancia().iniciar(new Usuario(1, "mlopez", "-", Rol.COORDINADOR, true));
        JugadorService jugadores = new JugadorService(new JugadorDAODesactualizado(new JugadorDAOMySQL(conexion)),
                new CategoriaDAOMySQL(conexion), EscenarioPrueba.RELOJ);
        Jugador duplicado = new Jugador("42587634", "Otro", "Puesto", LocalDate.of(2001, 5, 10), null, "Defensor",
                new CategoriaDAOMySQL(conexion).buscarPorId(1).orElseThrow());
        ValidacionException error = assertThrows(ValidacionException.class, () -> jugadores.registrar(duplicado));
        assertEquals(List.of("Ya existe un jugador con DNI 42587634."), error.getErrores());
    }

    /**
     * Simula la carrera entre dos puestos: la consulta previa por DNI no ve al
     * jugador (todavía no estaba), de modo que el servicio llega a insertar y es
     * el índice único {@code uq_jugador_dni} el que rechaza el alta.
     */
    private static final class JugadorDAODesactualizado implements JugadorDAO {

        private final JugadorDAO real;

        JugadorDAODesactualizado(JugadorDAO real) {
            this.real = real;
        }

        @Override
        public Optional<Jugador> buscarPorDni(String dni) {
            return Optional.empty();
        }

        @Override
        public List<Jugador> buscarPorApellido(String apellido) throws PersistenciaException {
            return real.buscarPorApellido(apellido);
        }

        @Override
        public List<Jugador> listarActivos() throws PersistenciaException {
            return real.listarActivos();
        }

        @Override
        public int insertar(Jugador jugador) throws PersistenciaException {
            return real.insertar(jugador);
        }

        @Override
        public void actualizar(Jugador jugador) throws PersistenciaException {
            real.actualizar(jugador);
        }

        @Override
        public Optional<Jugador> buscarPorId(int id) throws PersistenciaException {
            return real.buscarPorId(id);
        }

        @Override
        public List<Jugador> listarTodos() throws PersistenciaException {
            return real.listarTodos();
        }
    }
}
