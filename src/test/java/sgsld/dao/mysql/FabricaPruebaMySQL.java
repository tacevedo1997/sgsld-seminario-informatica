package sgsld.dao.mysql;

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
 * Fábrica provisoria de las pruebas de contrato mientras se completan los DAO
 * MySQL: ofrece los de catálogos, usuarios, categorías, jugadores y
 * kinesiólogos. La reemplaza {@code FabricaDAOMySQL}.
 */
public class FabricaPruebaMySQL extends FabricaDAO {

    private final ConexionBD conexion;

    /**
     * Crea la fábrica sobre una conexión.
     *
     * @param conexion conexión a {@code sgsld_test}
     */
    public FabricaPruebaMySQL(ConexionBD conexion) {
        this.conexion = conexion;
    }

    @Override
    public JugadorDAO getJugadorDAO() {
        return new JugadorDAOMySQL(conexion);
    }

    @Override
    public CategoriaDAO getCategoriaDAO() {
        return new CategoriaDAOMySQL(conexion);
    }

    @Override
    public KinesiologoDAO getKinesiologoDAO() {
        return new KinesiologoDAOMySQL(conexion);
    }

    @Override
    public UsuarioDAO getUsuarioDAO() {
        return new UsuarioDAOMySQL(conexion);
    }

    @Override
    public ZonaCorporalDAO getZonaCorporalDAO() {
        return new ZonaCorporalDAOMySQL(conexion);
    }

    @Override
    public TipoLesionDAO getTipoLesionDAO() {
        return new TipoLesionDAOMySQL(conexion);
    }

    @Override
    public LesionDAO getLesionDAO() {
        throw new UnsupportedOperationException("Pendiente: DAO de lesiones");
    }

    @Override
    public TratamientoDAO getTratamientoDAO() {
        throw new UnsupportedOperationException("Pendiente: DAO de tratamientos");
    }

    @Override
    public EvolucionDAO getEvolucionDAO() {
        throw new UnsupportedOperationException("Pendiente: DAO de evoluciones");
    }

    @Override
    public EstadoJugadorDAO getEstadoJugadorDAO() {
        throw new UnsupportedOperationException("Pendiente: DAO de estado");
    }

    @Override
    public ReporteDAO getReporteDAO() {
        throw new UnsupportedOperationException("Pendiente: DAO de reportes");
    }
}
