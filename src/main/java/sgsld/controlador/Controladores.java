package sgsld.controlador;

/**
 * Conjunto de controladores que la raíz de composición entrega a los menús.
 *
 * @param login         inicio y cierre de sesión (CU-09)
 * @param jugadores     gestión de jugadores (CU-01)
 * @param categorias    gestión de categorías (CU-02)
 * @param kinesiologos  gestión de kinesiólogos (CU-03)
 */
public record Controladores(LoginController login, JugadorController jugadores, CategoriaController categorias,
                            KinesiologoController kinesiologos) {
}
