package sgsld;

import java.time.Clock;
import java.time.LocalDate;

import sgsld.dao.FabricaDAO;
import sgsld.dao.memoria.DatosDePrueba;
import sgsld.dao.memoria.FabricaDAOMemoria;
import sgsld.servicio.AutenticacionService;
import sgsld.servicio.CategoriaService;
import sgsld.servicio.JugadorService;
import sgsld.servicio.KinesiologoService;
import sgsld.util.Fechas;
import sgsld.util.SesionUsuario;

/**
 * Escenario común de las pruebas: fábrica de DAO en memoria con el lote de la
 * sección 14.5 de la entrega 2, servicios con un reloj fijo en el 26/09/2026
 * (fecha del plan de pruebas) y sesión iniciada con el rol necesario.
 * <p>
 * Los DAO en memoria cumplen el rol de dobles de prueba que el plan asignaba
 * a Mockito. La sesión se inicia directamente (sin PBKDF2) para que cada
 * prueba no tarde medio segundo; el camino con contraseña lo cubre
 * {@code AutenticacionTest}.
 */
public final class EscenarioPrueba {

    /** Fecha fija de las pruebas. */
    public static final LocalDate HOY = LocalDate.of(2026, 9, 26);
    /** Reloj fijo en {@link #HOY}, zona Córdoba. */
    public static final Clock RELOJ = Clock.fixed(HOY.atStartOfDay(Fechas.ZONA).toInstant(), Fechas.ZONA);
    /** Contraseña de todos los usuarios del lote. */
    public static final String CONTRASENA_LOTE = "Sgsld2026!";

    private final FabricaDAO fabrica = new FabricaDAOMemoria();
    private final AutenticacionService autenticacion;
    private final JugadorService jugadores;
    private final CategoriaService categorias;
    private final KinesiologoService kinesiologos;

    /** Crea el escenario con el lote cargado y sin sesión iniciada. */
    public EscenarioPrueba() {
        DatosDePrueba.cargar(fabrica);
        autenticacion = new AutenticacionService(fabrica.getUsuarioDAO());
        jugadores = new JugadorService(fabrica.getJugadorDAO(), fabrica.getCategoriaDAO(), RELOJ);
        categorias = new CategoriaService(fabrica.getCategoriaDAO());
        kinesiologos = new KinesiologoService(fabrica.getKinesiologoDAO(), fabrica.getUsuarioDAO());
    }

    /**
     * Inicia sesión como el coordinador {@code mlopez}.
     *
     * @return este escenario
     */
    public EscenarioPrueba comoCoordinador() {
        return iniciarComo("mlopez");
    }

    /**
     * Inicia sesión como la kinesióloga {@code lfernandez}.
     *
     * @return este escenario
     */
    public EscenarioPrueba comoKinesiologo() {
        return iniciarComo("lfernandez");
    }

    /**
     * Inicia sesión como el cuerpo técnico {@code ctecnico}.
     *
     * @return este escenario
     */
    public EscenarioPrueba comoCuerpoTecnico() {
        return iniciarComo("ctecnico");
    }

    /** Cierra la sesión del Singleton para no arrastrarla a la prueba siguiente. */
    public static void cerrarSesion() {
        SesionUsuario.getInstancia().cerrar();
    }

    public FabricaDAO fabrica() {
        return fabrica;
    }

    public AutenticacionService autenticacion() {
        return autenticacion;
    }

    public JugadorService jugadores() {
        return jugadores;
    }

    public CategoriaService categorias() {
        return categorias;
    }

    public KinesiologoService kinesiologos() {
        return kinesiologos;
    }

    private EscenarioPrueba iniciarComo(String nombreUsuario) {
        SesionUsuario.getInstancia().iniciar(
                fabrica.getUsuarioDAO().buscarPorNombreUsuario(nombreUsuario).orElseThrow());
        return this;
    }
}
