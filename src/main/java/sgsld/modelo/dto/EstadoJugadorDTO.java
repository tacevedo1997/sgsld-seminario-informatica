package sgsld.modelo.dto;

import sgsld.modelo.EstadoDisponibilidad;

/**
 * Fila del estado de disponibilidad (equivale a la vista {@code v_estado_jugador}).
 * No incluye información clínica: es lo único que ve el Cuerpo Técnico.
 *
 * @param idJugador identificador del jugador
 * @param jugador   "Apellido, Nombre"
 * @param categoria nombre de la categoría vigente
 * @param estado    disponibilidad derivada
 */
public record EstadoJugadorDTO(int idJugador, String jugador, String categoria, EstadoDisponibilidad estado) {
}
