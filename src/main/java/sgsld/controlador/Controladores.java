package sgsld.controlador;

/**
 * Conjunto de controladores que la raíz de composición entrega a los menús.
 *
 * @param login          inicio y cierre de sesión (CU-09)
 * @param jugadores      gestión de jugadores (CU-01)
 * @param categorias     gestión de categorías (CU-02)
 * @param kinesiologos   gestión de kinesiólogos (CU-03)
 * @param registroLesion registro de lesiones con historial y alerta (CU-04, CU-12, CU-11)
 * @param tratamientos   tratamientos y evoluciones (CU-05, CU-06)
 * @param fichas         ficha individual del jugador (CU-10)
 * @param estadoActual   estado de disponibilidad (CU-08)
 * @param reportes       reportes estadísticos (CU-07)
 */
public record Controladores(LoginController login, JugadorController jugadores, CategoriaController categorias,
                            KinesiologoController kinesiologos, RegistroLesionController registroLesion,
                            TratamientoController tratamientos, FichaJugadorController fichas,
                            EstadoActualController estadoActual, ReporteController reportes) {
}
