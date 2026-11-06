package sgsld.dao.mysql;

import sgsld.dao.FabricaDAO;

/**
 * Fábrica concreta de DAO MySQL (estrategia "Factory for DAOs"). Crea una
 * instancia de cada DAO sobre la misma {@link ConexionBD}; cada operación abre
 * y cierra su propia conexión.
 */
public class FabricaDAOMySQL extends FabricaDAO {

    private final JugadorDAOMySQL jugadorDAO;
    private final CategoriaDAOMySQL categoriaDAO;
    private final KinesiologoDAOMySQL kinesiologoDAO;
    private final UsuarioDAOMySQL usuarioDAO;
    private final ZonaCorporalDAOMySQL zonaCorporalDAO;
    private final TipoLesionDAOMySQL tipoLesionDAO;
    private final LesionDAOMySQL lesionDAO;
    private final TratamientoDAOMySQL tratamientoDAO;
    private final EvolucionDAOMySQL evolucionDAO;
    private final EstadoJugadorDAOMySQL estadoJugadorDAO;
    private final ReporteDAOMySQL reporteDAO;

    /**
     * Crea la fábrica.
     *
     * @param conexion conexión configurada (en la aplicación, {@link ConexionBD#getInstancia()})
     */
    public FabricaDAOMySQL(ConexionBD conexion) {
        jugadorDAO = new JugadorDAOMySQL(conexion);
        categoriaDAO = new CategoriaDAOMySQL(conexion);
        kinesiologoDAO = new KinesiologoDAOMySQL(conexion);
        usuarioDAO = new UsuarioDAOMySQL(conexion);
        zonaCorporalDAO = new ZonaCorporalDAOMySQL(conexion);
        tipoLesionDAO = new TipoLesionDAOMySQL(conexion);
        lesionDAO = new LesionDAOMySQL(conexion);
        tratamientoDAO = new TratamientoDAOMySQL(conexion);
        evolucionDAO = new EvolucionDAOMySQL(conexion);
        estadoJugadorDAO = new EstadoJugadorDAOMySQL(conexion);
        reporteDAO = new ReporteDAOMySQL(conexion);
    }

    @Override
    public JugadorDAOMySQL getJugadorDAO() {
        return jugadorDAO;
    }

    @Override
    public CategoriaDAOMySQL getCategoriaDAO() {
        return categoriaDAO;
    }

    @Override
    public KinesiologoDAOMySQL getKinesiologoDAO() {
        return kinesiologoDAO;
    }

    @Override
    public UsuarioDAOMySQL getUsuarioDAO() {
        return usuarioDAO;
    }

    @Override
    public ZonaCorporalDAOMySQL getZonaCorporalDAO() {
        return zonaCorporalDAO;
    }

    @Override
    public TipoLesionDAOMySQL getTipoLesionDAO() {
        return tipoLesionDAO;
    }

    @Override
    public LesionDAOMySQL getLesionDAO() {
        return lesionDAO;
    }

    @Override
    public TratamientoDAOMySQL getTratamientoDAO() {
        return tratamientoDAO;
    }

    @Override
    public EvolucionDAOMySQL getEvolucionDAO() {
        return evolucionDAO;
    }

    @Override
    public EstadoJugadorDAOMySQL getEstadoJugadorDAO() {
        return estadoJugadorDAO;
    }

    @Override
    public ReporteDAOMySQL getReporteDAO() {
        return reporteDAO;
    }
}
