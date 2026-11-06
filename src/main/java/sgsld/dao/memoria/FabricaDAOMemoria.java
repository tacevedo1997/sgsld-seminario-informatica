package sgsld.dao.memoria;

import sgsld.dao.FabricaDAO;

/**
 * Fábrica concreta de DAO en memoria. Crea una única instancia de cada DAO,
 * de modo que todos comparten las mismas "tablas" mientras dura la ejecución.
 * <p>
 * Los getters redefinen el tipo de retorno con la clase concreta (retorno
 * covariante): el código propio de la memoria, como {@link DatosDePrueba} y
 * las pruebas, usa DAO que nunca lanzan {@code PersistenciaException} y no
 * necesita capturarla.
 */
public class FabricaDAOMemoria extends FabricaDAO {

    private final JugadorDAOMemoria jugadorDAO = new JugadorDAOMemoria();
    private final CategoriaDAOMemoria categoriaDAO = new CategoriaDAOMemoria();
    private final UsuarioDAOMemoria usuarioDAO = new UsuarioDAOMemoria();
    private final KinesiologoDAOMemoria kinesiologoDAO = new KinesiologoDAOMemoria(usuarioDAO);
    private final ZonaCorporalDAOMemoria zonaCorporalDAO = new ZonaCorporalDAOMemoria();
    private final TipoLesionDAOMemoria tipoLesionDAO = new TipoLesionDAOMemoria();
    private final LesionDAOMemoria lesionDAO = new LesionDAOMemoria();
    private final EvolucionDAOMemoria evolucionDAO = new EvolucionDAOMemoria();
    private final TratamientoDAOMemoria tratamientoDAO = new TratamientoDAOMemoria(evolucionDAO);
    private final EstadoJugadorDAOMemoria estadoJugadorDAO = new EstadoJugadorDAOMemoria(jugadorDAO, tratamientoDAO);
    private final ReporteDAOMemoria reporteDAO = new ReporteDAOMemoria(lesionDAO, tratamientoDAO);

    @Override
    public JugadorDAOMemoria getJugadorDAO() {
        return jugadorDAO;
    }

    @Override
    public CategoriaDAOMemoria getCategoriaDAO() {
        return categoriaDAO;
    }

    @Override
    public KinesiologoDAOMemoria getKinesiologoDAO() {
        return kinesiologoDAO;
    }

    @Override
    public UsuarioDAOMemoria getUsuarioDAO() {
        return usuarioDAO;
    }

    @Override
    public ZonaCorporalDAOMemoria getZonaCorporalDAO() {
        return zonaCorporalDAO;
    }

    @Override
    public TipoLesionDAOMemoria getTipoLesionDAO() {
        return tipoLesionDAO;
    }

    @Override
    public LesionDAOMemoria getLesionDAO() {
        return lesionDAO;
    }

    @Override
    public TratamientoDAOMemoria getTratamientoDAO() {
        return tratamientoDAO;
    }

    @Override
    public EvolucionDAOMemoria getEvolucionDAO() {
        return evolucionDAO;
    }

    @Override
    public EstadoJugadorDAOMemoria getEstadoJugadorDAO() {
        return estadoJugadorDAO;
    }

    @Override
    public ReporteDAOMemoria getReporteDAO() {
        return reporteDAO;
    }
}
