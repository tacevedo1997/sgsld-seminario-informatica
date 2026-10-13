package sgsld.servicio;

import java.util.Comparator;
import java.util.List;

import sgsld.dao.EstadoJugadorDAO;
import sgsld.dao.JugadorDAO;
import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Rol;
import sgsld.modelo.dto.EstadoJugadorDTO;
import sgsld.modelo.dto.FichaJugadorDTO;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.Algoritmos;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.SesionUsuario;
import sgsld.util.Texto;

/**
 * Consultas: estado de disponibilidad (CU-08), para cualquier rol, y ficha
 * individual (CU-10), solo para Coordinador y Kinesiólogo. El Cuerpo Técnico
 * no accede a información clínica (RF12, RNF13).
 */
public class ConsultaService {

    /** Orden de la consulta C3: categoría y jugador, alfabético español. */
    private static final Comparator<EstadoJugadorDTO> POR_CATEGORIA_Y_JUGADOR =
            Comparator.comparing(EstadoJugadorDTO::categoria, Texto.ordenAlfabetico())
                    .thenComparing(EstadoJugadorDTO::jugador, Texto.ordenAlfabetico());

    private final EstadoJugadorDAO estados;
    private final JugadorDAO jugadores;
    private final LesionService lesiones;

    /**
     * Crea el servicio.
     *
     * @param estados   DAO del estado de disponibilidad
     * @param jugadores DAO de jugadores
     * @param lesiones  servicio de lesiones, para el historial de la ficha
     */
    public ConsultaService(EstadoJugadorDAO estados, JugadorDAO jugadores, LesionService lesiones) {
        this.estados = estados;
        this.jugadores = jugadores;
        this.lesiones = lesiones;
    }

    /**
     * Estado actual de los jugadores activos (CU-08), ordenado por categoría y
     * jugador. No incluye datos clínicos.
     *
     * @return una fila por jugador activo
     * @throws AccesoDenegadoException si no hay sesión iniciada
     */
    public List<EstadoJugadorDTO> consultarEstadoActual() throws AccesoDenegadoException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR, Rol.KINESIOLOGO, Rol.CUERPO_TECNICO);
        List<EstadoJugadorDTO> filas = estados.listarEstados();
        Algoritmos.ordenarPorInsercion(filas, POR_CATEGORIA_Y_JUGADOR);
        return filas;
    }

    /**
     * Estado de disponibilidad de un jugador.
     *
     * @param idJugador identificador del jugador
     * @return su estado
     * @throws AccesoDenegadoException si no hay sesión iniciada
     */
    public EstadoDisponibilidad consultarEstado(int idJugador) throws AccesoDenegadoException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR, Rol.KINESIOLOGO, Rol.CUERPO_TECNICO);
        return estados.estadoDe(idJugador);
    }

    /**
     * Ficha individual de un jugador (CU-10): datos básicos, estado actual e
     * historial clínico ordenado.
     *
     * @param idJugador identificador del jugador
     * @return la ficha
     * @throws AccesoDenegadoException      si el rol es Cuerpo Técnico (CPS01) o no hay sesión
     * @throws EntidadNoEncontradaException si el jugador no existe
     */
    public FichaJugadorDTO consultarFicha(int idJugador)
            throws AccesoDenegadoException, EntidadNoEncontradaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR, Rol.KINESIOLOGO);
        Jugador jugador = jugadores.buscarPorId(idJugador).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró el jugador N° " + idJugador + "."));
        return new FichaJugadorDTO(jugador, estados.estadoDe(idJugador), lesiones.consultarHistorial(idJugador));
    }
}
