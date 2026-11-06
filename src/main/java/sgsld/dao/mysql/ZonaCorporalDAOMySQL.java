package sgsld.dao.mysql;

import java.util.List;
import java.util.Optional;

import sgsld.dao.ZonaCorporalDAO;
import sgsld.modelo.ZonaCorporal;
import sgsld.util.PersistenciaException;

/** Catálogo de zonas corporales en MySQL (tabla {@code zona_corporal}). */
public class ZonaCorporalDAOMySQL extends AccesoDatosMySQL implements ZonaCorporalDAO {

    private static final String SELECCIONAR = "SELECT " + Mapeos.COLUMNAS_ZONA + " FROM zona_corporal z";
    private static final String INSERTAR = "INSERT INTO zona_corporal (nombre, region) VALUES (?, ?)";
    private static final String ACTUALIZAR = "UPDATE zona_corporal SET nombre = ?, region = ? WHERE id_zona = ?";

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public ZonaCorporalDAOMySQL(ConexionBD conexion) {
        super(conexion);
    }

    @Override
    public int insertar(ZonaCorporal zona) throws PersistenciaException {
        exigirNueva(zona);
        zona.setId(insertar(INSERTAR, zona.getNombre(), zona.getRegion()));
        return zona.getId();
    }

    @Override
    public void actualizar(ZonaCorporal zona) throws PersistenciaException {
        exigirExistente(actualizar(ACTUALIZAR, zona.getNombre(), zona.getRegion(), zona.getId()), zona);
    }

    @Override
    public Optional<ZonaCorporal> buscarPorId(int id) throws PersistenciaException {
        return consultarUno(SELECCIONAR + " WHERE z.id_zona = ?", Mapeos::zona, id);
    }

    @Override
    public List<ZonaCorporal> listarTodos() throws PersistenciaException {
        return consultar(SELECCIONAR + " ORDER BY z.id_zona", Mapeos::zona);
    }
}
