package sgsld.dao.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import sgsld.dao.TratamientoDAO;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Lesion;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.Rol;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.Usuario;
import sgsld.servicio.JugadorService;
import sgsld.servicio.TratamientoService;
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
     * Fuerza un error <b>después</b> de insertar la evolución: el fin
     * 01/06/2026 es anterior al inicio del tratamiento 7 (16/06/2026), de modo
     * que el {@code UPDATE} viola {@code ck_tratamiento_fechas} (error 3819,
     * prueba P3 del script). Se llama al DAO directamente porque el servicio
     * rechazaría esa fecha antes de llegar a la base.
     */
    @Test
    @DisplayName("Rollback: registrar y finalizar con fin anterior al inicio (CHECK tras insertar la evolución) no deja nada")
    void rollbackDeRegistrarEvolucionYFinalizar() throws Exception {
        TratamientoDAOMySQL tratamientos = new TratamientoDAOMySQL(conexion);
        Evolucion evolucion = new Evolucion(LocalDate.of(2026, 9, 20), "Evolución que el rollback debe deshacer",
                ResultadoEvolucion.FAVORABLE);
        PersistenciaException error = assertThrows(PersistenciaException.class,
                () -> tratamientos.registrarEvolucionYFinalizar(7, evolucion, LocalDate.of(2026, 6, 1)));
        assertEquals(Motivo.RESTRICCION, error.getMotivo());
        assertEquals("ck_tratamiento_fechas", error.getRestriccion());
        assertEquals(0, evolucion.getId());

        Tratamiento tratamiento7 = tratamientos.buscarPorId(7).orElseThrow();
        assertTrue(tratamiento7.estaEnCurso());
        assertNull(tratamiento7.getFechaFin());
        assertEquals(4, tratamiento7.getCantidadEvoluciones());
        for (Evolucion guardada : new EvolucionDAOMySQL(conexion).buscarPorTratamiento(7)) {
            assertNotEquals(evolucion.getObservacion(), guardada.getObservacion());
        }
    }

    /**
     * El tratamiento 2 ya está finalizado (como si otro puesto lo hubiera
     * finalizado después de la validación): la evolución se inserta, el
     * {@code UPDATE} no encuentra el tratamiento en curso y la excepción
     * {@code CONFLICTO} también provoca el rollback.
     */
    @Test
    @DisplayName("Rollback: registrar y finalizar un tratamiento ya finalizado (CONFLICTO tras insertar la evolución) no la deja")
    void rollbackDeRegistrarEvolucionYFinalizarPorConflicto() throws Exception {
        TratamientoDAOMySQL tratamientos = new TratamientoDAOMySQL(conexion);
        Evolucion evolucion = new Evolucion(LocalDate.of(2026, 9, 1), "Evolución sobre un tratamiento ya finalizado",
                ResultadoEvolucion.ESTABLE);
        PersistenciaException error = assertThrows(PersistenciaException.class,
                () -> tratamientos.registrarEvolucionYFinalizar(2, evolucion, LocalDate.of(2026, 9, 1)));
        assertEquals(Motivo.CONFLICTO, error.getMotivo());
        assertEquals(0, evolucion.getId());
        assertEquals(3, new EvolucionDAOMySQL(conexion).buscarPorTratamiento(2).size());
        assertEquals(LocalDate.of(2026, 2, 20), tratamientos.buscarPorId(2).orElseThrow().getFechaFin());
    }

    @Test
    @DisplayName("Concurrencia: tratamiento en curso que otro puesto registró después de la validación → mismo ValidacionException")
    void tratamientoEnCursoDetectadoPorLaBase() throws Exception {
        SesionUsuario.getInstancia().iniciar(new Usuario(2, "lfernandez", "-", Rol.KINESIOLOGO, true));
        LesionDAOMySQL lesiones = new LesionDAOMySQL(conexion);
        KinesiologoDAOMySQL kinesiologos = new KinesiologoDAOMySQL(conexion);
        TratamientoService servicio = new TratamientoService(
                new TratamientoDAODesactualizado(new TratamientoDAOMySQL(conexion)), lesiones, kinesiologos,
                new EvolucionDAOMySQL(conexion), EscenarioPrueba.RELOJ);
        Lesion lesion7 = lesiones.buscarPorId(7).orElseThrow();
        Tratamiento segundo = new Tratamiento(lesion7, kinesiologos.buscarPorId(1).orElseThrow(),
                LocalDate.of(2026, 9, 20), "Readaptación funcional", null);
        ValidacionException error = assertThrows(ValidacionException.class, () -> servicio.registrarTratamiento(segundo));
        assertEquals(List.of("La lesión N° 7 ya tiene un tratamiento en curso. "
                + "Debe finalizarlo antes de registrar uno nuevo."), error.getErrores());
        assertEquals(0, segundo.getId());
        assertEquals(1, lesion7.getTratamientos().size());
        assertEquals(1, lesiones.buscarPorId(7).orElseThrow().getTratamientos().size());
    }

    @Test
    @DisplayName("Concurrencia: finalizar un tratamiento que otro puesto ya finalizó → CONFLICTO y sin cambios")
    void finalizarTratamientoYaFinalizado() throws Exception {
        TratamientoDAOMySQL tratamientos = new TratamientoDAOMySQL(conexion);
        PersistenciaException error = assertThrows(PersistenciaException.class,
                () -> tratamientos.finalizar(2, LocalDate.of(2026, 9, 1)));
        assertEquals(Motivo.CONFLICTO, error.getMotivo());
        assertEquals(LocalDate.of(2026, 2, 20), tratamientos.buscarPorId(2).orElseThrow().getFechaFin());
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

    /**
     * Simula la carrera entre dos puestos al registrar un tratamiento: la
     * consulta previa no ve el tratamiento en curso, de modo que es el índice
     * único {@code uq_tratamiento_en_curso} el que rechaza el alta.
     */
    private static final class TratamientoDAODesactualizado implements TratamientoDAO {

        private final TratamientoDAO real;

        TratamientoDAODesactualizado(TratamientoDAO real) {
            this.real = real;
        }

        @Override
        public boolean existeActivo(int idLesion) {
            return false;
        }

        @Override
        public void finalizar(int idTratamiento, LocalDate fechaFin) throws PersistenciaException {
            real.finalizar(idTratamiento, fechaFin);
        }

        @Override
        public int registrarEvolucionYFinalizar(int idTratamiento, Evolucion evolucion, LocalDate fechaFin)
                throws PersistenciaException {
            return real.registrarEvolucionYFinalizar(idTratamiento, evolucion, fechaFin);
        }

        @Override
        public List<Tratamiento> listarEnCurso() throws PersistenciaException {
            return real.listarEnCurso();
        }

        @Override
        public int insertar(Tratamiento tratamiento) throws PersistenciaException {
            return real.insertar(tratamiento);
        }

        @Override
        public void actualizar(Tratamiento tratamiento) throws PersistenciaException {
            real.actualizar(tratamiento);
        }

        @Override
        public Optional<Tratamiento> buscarPorId(int id) throws PersistenciaException {
            return real.buscarPorId(id);
        }

        @Override
        public List<Tratamiento> listarTodos() throws PersistenciaException {
            return real.listarTodos();
        }
    }
}