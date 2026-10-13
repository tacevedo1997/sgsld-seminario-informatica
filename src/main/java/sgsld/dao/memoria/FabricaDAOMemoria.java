package sgsld.dao.memoria;

import sgsld.dao.CategoriaDAO;
import sgsld.dao.EstadoJugadorDAO;
import sgsld.dao.EvolucionDAO;
import sgsld.dao.FabricaDAO;
import sgsld.dao.JugadorDAO;
import sgsld.dao.KinesiologoDAO;
import sgsld.dao.LesionDAO;
import sgsld.dao.ReporteDAO;
import sgsld.dao.TipoLesionDAO;
import sgsld.dao.TratamientoDAO;
import sgsld.dao.UsuarioDAO;
import sgsld.dao.ZonaCorporalDAO;

/**
 * Fábrica concreta de DAO en memoria. Crea una única instancia de cada DAO,
 * de modo que todos comparten las mismas "tablas" mientras dura la ejecución.
 */
public class FabricaDAOMemoria extends FabricaDAO {

    private final JugadorDAO jugadorDAO = new JugadorDAOMemoria();
    private final CategoriaDAO categoriaDAO = new CategoriaDAOMemoria();
    private final KinesiologoDAO kinesiologoDAO = new KinesiologoDAOMemoria();
    private final UsuarioDAO usuarioDAO = new UsuarioDAOMemoria();
    private final ZonaCorporalDAO zonaCorporalDAO = new ZonaCorporalDAOMemoria();
    private final TipoLesionDAO tipoLesionDAO = new TipoLesionDAOMemoria();
    private final LesionDAO lesionDAO = new LesionDAOMemoria();
    private final TratamientoDAO tratamientoDAO = new TratamientoDAOMemoria();
    private final EvolucionDAO evolucionDAO = new EvolucionDAOMemoria();
    private final EstadoJugadorDAO estadoJugadorDAO = new EstadoJugadorDAOMemoria(jugadorDAO, tratamientoDAO);
    private final ReporteDAO reporteDAO = new ReporteDAOMemoria(lesionDAO, tratamientoDAO);

    @Override
    public JugadorDAO getJugadorDAO() {
        return jugadorDAO;
    }

    @Override
    public CategoriaDAO getCategoriaDAO() {
        return categoriaDAO;
    }

    @Override
    public KinesiologoDAO getKinesiologoDAO() {
        return kinesiologoDAO;
    }

    @Override
    public UsuarioDAO getUsuarioDAO() {
        return usuarioDAO;
    }

    @Override
    public ZonaCorporalDAO getZonaCorporalDAO() {
        return zonaCorporalDAO;
    }

    @Override
    public TipoLesionDAO getTipoLesionDAO() {
        return tipoLesionDAO;
    }

    @Override
    public LesionDAO getLesionDAO() {
        return lesionDAO;
    }

    @Override
    public TratamientoDAO getTratamientoDAO() {
        return tratamientoDAO;
    }

    @Override
    public EvolucionDAO getEvolucionDAO() {
        return evolucionDAO;
    }

    @Override
    public EstadoJugadorDAO getEstadoJugadorDAO() {
        return estadoJugadorDAO;
    }

    @Override
    public ReporteDAO getReporteDAO() {
        return reporteDAO;
    }
}
