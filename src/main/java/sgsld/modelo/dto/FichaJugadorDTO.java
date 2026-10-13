package sgsld.modelo.dto;

import java.util.List;

import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Lesion;

/**
 * Ficha individual de un jugador (CU-10): datos básicos, estado actual e
 * historial clínico en orden cronológico.
 *
 * @param jugador   jugador consultado
 * @param estado    disponibilidad actual
 * @param historial lesiones con sus tratamientos y evoluciones
 */
public record FichaJugadorDTO(Jugador jugador, EstadoDisponibilidad estado, List<Lesion> historial) {

    /** Copia el historial en una lista no modificable. */
    public FichaJugadorDTO {
        historial = List.copyOf(historial);
    }
}
