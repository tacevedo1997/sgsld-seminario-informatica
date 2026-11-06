package sgsld.servicio;

import java.time.Clock;

import sgsld.dao.FabricaDAO;

/**
 * Los servicios del sistema, armados sobre una fábrica de DAO. Las dos vistas
 * (consola y JavaFX) usan los mismos servicios sobre el mismo modelo: es la
 * separación del patrón MVC.
 *
 * @param autenticacion inicio y cierre de sesión (CU-09)
 * @param jugadores     gestión de jugadores (CU-01)
 * @param categorias    gestión de categorías (CU-02)
 * @param kinesiologos  gestión de kinesiólogos (CU-03)
 * @param lesiones      lesiones, historial y recurrencia (CU-04, CU-11, CU-12)
 * @param tratamientos  tratamientos y evoluciones (CU-05, CU-06)
 * @param consultas     estado actual y ficha (CU-08, CU-10)
 * @param reportes      reportes estadísticos (CU-07)
 */
public record Servicios(AutenticacionService autenticacion, JugadorService jugadores, CategoriaService categorias,
                        KinesiologoService kinesiologos, LesionService lesiones, TratamientoService tratamientos,
                        ConsultaService consultas, ReporteService reportes) {

    /**
     * Crea los servicios sobre los DAO de una fábrica, con el reloj indicado.
     *
     * @param fabrica fábrica de DAO (en memoria o MySQL)
     * @param reloj   reloj que define la fecha actual
     * @return los servicios
     */
    public static Servicios crear(FabricaDAO fabrica, Clock reloj) {
        LesionService lesiones = new LesionService(fabrica.getLesionDAO(), fabrica.getJugadorDAO(),
                fabrica.getZonaCorporalDAO(), fabrica.getTipoLesionDAO(), reloj);
        return new Servicios(
                new AutenticacionService(fabrica.getUsuarioDAO()),
                new JugadorService(fabrica.getJugadorDAO(), fabrica.getCategoriaDAO(), reloj),
                new CategoriaService(fabrica.getCategoriaDAO()),
                new KinesiologoService(fabrica.getKinesiologoDAO(), fabrica.getUsuarioDAO()),
                lesiones,
                new TratamientoService(fabrica.getTratamientoDAO(), fabrica.getLesionDAO(), fabrica.getKinesiologoDAO(),
                        fabrica.getEvolucionDAO(), reloj),
                new ConsultaService(fabrica.getEstadoJugadorDAO(), fabrica.getJugadorDAO(), lesiones, reloj),
                new ReporteService(fabrica.getReporteDAO(), reloj));
    }
}
