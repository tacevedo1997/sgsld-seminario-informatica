package sgsld.dao;

/**
 * Fábrica abstracta de DAO (estrategia "Factory for DAOs" del patrón DAO,
 * Alur et al., 2003).
 * <p>
 * Los servicios y la raíz de composición solo conocen esta abstracción y las
 * interfaces DAO. La implementación actual es {@code FabricaDAOMemoria}; en la
 * entrega final se agregará {@code FabricaDAOMySQL} sin cambiar el resto del
 * sistema.
 */
public abstract class FabricaDAO {

    /**
     * Devuelve el DAO de jugadores.
     *
     * @return el DAO de jugadores
     */
    public abstract JugadorDAO getJugadorDAO();

    /**
     * Devuelve el DAO de categorías.
     *
     * @return el DAO de categorías
     */
    public abstract CategoriaDAO getCategoriaDAO();

    /**
     * Devuelve el DAO de kinesiólogos.
     *
     * @return el DAO de kinesiólogos
     */
    public abstract KinesiologoDAO getKinesiologoDAO();

    /**
     * Devuelve el DAO de usuarios.
     *
     * @return el DAO de usuarios
     */
    public abstract UsuarioDAO getUsuarioDAO();

    /**
     * Devuelve el DAO de zonas corporales.
     *
     * @return el DAO de zonas corporales
     */
    public abstract ZonaCorporalDAO getZonaCorporalDAO();

    /**
     * Devuelve el DAO de tipos de lesión.
     *
     * @return el DAO de tipos de lesión
     */
    public abstract TipoLesionDAO getTipoLesionDAO();

    /**
     * Devuelve el DAO de lesiones.
     *
     * @return el DAO de lesiones
     */
    public abstract LesionDAO getLesionDAO();

    /**
     * Devuelve el DAO de tratamientos.
     *
     * @return el DAO de tratamientos
     */
    public abstract TratamientoDAO getTratamientoDAO();

    /**
     * Devuelve el DAO de evoluciones.
     *
     * @return el DAO de evoluciones
     */
    public abstract EvolucionDAO getEvolucionDAO();

    /**
     * Devuelve el DAO del estado de disponibilidad.
     *
     * @return el DAO del estado de disponibilidad
     */
    public abstract EstadoJugadorDAO getEstadoJugadorDAO();

    /**
     * Devuelve el DAO de consultas para reportes.
     *
     * @return el DAO de reportes
     */
    public abstract ReporteDAO getReporteDAO();
}
