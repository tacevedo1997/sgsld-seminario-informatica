package sgsld.dao.mysql;

import java.util.List;
import java.util.Optional;

import sgsld.dao.EstadoJugadorDAO;
import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.EstadoTratamiento;
import sgsld.modelo.dto.EstadoJugadorDTO;
import sgsld.util.PersistenciaException;

/**
 * Estado de disponibilidad en MySQL: consulta la vista {@code v_estado_jugador}
 * del script, que deriva el estado sin almacenarlo.
 */
public class EstadoJugadorDAOMySQL extends AccesoDatosMySQL implements EstadoJugadorDAO {

    private static final String VISTA = "SELECT id_jugador, jugador, categoria, estado FROM v_estado_jugador";
    /** La vista solo incluye jugadores activos; para un jugador dado de baja se aplica la misma regla. */
    private static final String EN_TRATAMIENTO = "SELECT EXISTS (SELECT 1 FROM lesion l "
            + "JOIN tratamiento t ON t.id_lesion = l.id_lesion WHERE l.id_jugador = ? AND t.estado = ?)";

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public EstadoJugadorDAOMySQL(ConexionBD conexion) {
        super(conexion);
    }

    @Override
    public List<EstadoJugadorDTO> listarEstados() throws PersistenciaException {
        return consultar(VISTA, fila -> new EstadoJugadorDTO(fila.getInt("id_jugador"), fila.getString("jugador"),
                fila.getString("categoria"), ValoresEnum.estadoDisponibilidad(fila.getString("estado"))));
    }

    @Override
    public EstadoDisponibilidad estadoDe(int idJugador) throws PersistenciaException {
        Optional<EstadoDisponibilidad> enVista = consultarUno(VISTA + " WHERE id_jugador = ?",
                fila -> ValoresEnum.estadoDisponibilidad(fila.getString("estado")), idJugador);
        if (enVista.isPresent()) {
            return enVista.get();
        }
        boolean enTratamiento = consultarUno(EN_TRATAMIENTO, fila -> fila.getBoolean(1), idJugador,
                EstadoTratamiento.EN_CURSO).orElse(false);
        return enTratamiento ? EstadoDisponibilidad.EN_TRATAMIENTO : EstadoDisponibilidad.DISPONIBLE;
    }
}
