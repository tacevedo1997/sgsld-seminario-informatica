package sgsld.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import sgsld.dao.JugadorDAO;
import sgsld.dao.LesionDAO;
import sgsld.dao.TipoLesionDAO;
import sgsld.dao.ZonaCorporalDAO;
import sgsld.modelo.Jugador;
import sgsld.modelo.Lesion;
import sgsld.modelo.Rol;
import sgsld.modelo.TipoLesion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.ZonaCorporal;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.Algoritmos;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.Fechas;
import sgsld.util.PersistenciaException;
import sgsld.util.SesionUsuario;
import sgsld.util.Validaciones;
import sgsld.util.ValidacionException;

/**
 * Registro de lesiones (CU-04), historial (CU-12) y alerta de recurrencia
 * (CU-11, RF06). Realiza la clase {@code GestorLesiones} del diseño.
 * <p>
 * Rol: Kinesiólogo; {@link #consultarHistorial(int)} también Coordinador.
 */
public class LesionService {

    /** Ventana de la regla de recurrencia, en meses. */
    public static final int MESES_VENTANA = 6;

    /** Orden cronológico de las lesiones (estable: ante igual fecha, conserva el orden de registro). */
    private static final Comparator<Lesion> POR_FECHA = Comparator.comparing(Lesion::getFecha);

    private final LesionDAO lesiones;
    private final JugadorDAO jugadores;
    private final ZonaCorporalDAO zonas;
    private final TipoLesionDAO tipos;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param lesiones  DAO de lesiones
     * @param jugadores DAO de jugadores
     * @param zonas     DAO del catálogo de zonas corporales
     * @param tipos     DAO del catálogo de tipos de lesión
     * @param reloj     reloj que define la fecha actual
     */
    public LesionService(LesionDAO lesiones, JugadorDAO jugadores, ZonaCorporalDAO zonas,
                         TipoLesionDAO tipos, Clock reloj) {
        this.lesiones = lesiones;
        this.jugadores = jugadores;
        this.zonas = zonas;
        this.tipos = tipos;
        this.reloj = reloj;
    }

    /**
     * Historial clínico de un jugador (CU-12): lesiones en orden cronológico,
     * con sus tratamientos y evoluciones también ordenados por fecha.
     *
     * @param idJugador identificador del jugador
     * @return las lesiones del jugador; vacía si no tiene
     * @throws AccesoDenegadoException      si el rol no es Kinesiólogo ni Coordinador
     * @throws EntidadNoEncontradaException si el jugador no existe
     */
    public List<Lesion> consultarHistorial(int idJugador)
            throws AccesoDenegadoException, EntidadNoEncontradaException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO, Rol.COORDINADOR);
        if (jugadores.buscarPorId(idJugador).isEmpty()) {
            throw new EntidadNoEncontradaException("No se encontró el jugador N° " + idJugador + ".");
        }
        List<Lesion> historial = lesiones.buscarPorJugador(idJugador);
        Algoritmos.ordenarPorInsercion(historial, POR_FECHA);
        for (Lesion lesion : historial) {
            lesion.ordenarTratamientos();
        }
        return historial;
    }

    /**
     * Valida los datos de una lesión: jugador activo; fecha obligatoria y no
     * futura (CU-04, 4a); tipo, zona y gravedad obligatorios (4b); descripción
     * de hasta 500 caracteres.
     *
     * @param lesion lesión a validar
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     * @throws ValidacionException     con todos los errores encontrados
     */
    public void validar(Lesion lesion) throws AccesoDenegadoException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        List<String> errores = new ArrayList<>();
        Jugador jugador = lesion.getJugador();
        if (jugador == null) {
            errores.add("Debe seleccionar un jugador.");
        } else if (jugadores.buscarPorId(jugador.getId()).isEmpty()) {
            errores.add("El jugador seleccionado no existe.");
        } else if (!jugador.isActivo()) {
            errores.add("El jugador está dado de baja.");
        }
        Validaciones.obligatorio(errores, lesion.getFecha(), "fecha");
        if (lesion.getFecha() != null && lesion.getFecha().isAfter(hoy())) {
            errores.add("La fecha de la lesión no puede ser posterior a la fecha actual ("
                    + Fechas.formatear(hoy()) + ").");
        }
        Validaciones.obligatorio(errores, lesion.getTipo(), "tipo de lesión");
        Validaciones.obligatorio(errores, lesion.getZona(), "zona corporal");
        Validaciones.obligatorio(errores, lesion.getGravedad(), "gravedad");
        Validaciones.texto(errores, lesion.getDescripcion(), "descripción", false, Lesion.MAX_DESCRIPCION);
        ValidacionException.lanzarSiHayErrores(errores);
    }

    /**
     * Antecedentes de una lesión nueva: historial del jugador filtrado a las
     * lesiones con fecha <b>estrictamente anterior</b> a la nueva (equivale a
     * {@code l.fecha < @fecha_nueva} de la consulta C2). El filtro se hace aquí
     * para no agregar una sexta decisión a {@link #verificarRecurrencia}.
     *
     * @param nueva lesión a registrar (con jugador y fecha)
     * @return los antecedentes en orden cronológico
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     */
    public List<Lesion> buscarAntecedentes(Lesion nueva) throws AccesoDenegadoException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        List<Lesion> antecedentes = new ArrayList<>();
        for (Lesion lesion : lesiones.buscarPorJugador(nueva.getJugador().getId())) {
            if (lesion.getFecha().isBefore(nueva.getFecha())) {
                antecedentes.add(lesion);
            }
        }
        Algoritmos.ordenarPorInsercion(antecedentes, POR_FECHA);
        return antecedentes;
    }

    /**
     * Regla de recurrencia (RF06): devuelve el primer antecedente en la misma
     * zona corporal cuyo último tratamiento no existe, sigue en curso o
     * finalizó en una fecha igual o posterior a la de la nueva lesión menos
     * seis meses. El límite se calcula con {@code minusMonths}, igual que
     * {@code DATE_SUB(..., INTERVAL 6 MONTH)} en la base. La alerta es
     * informativa y no bloquea el registro.
     * <p>
     * Tiene exactamente cinco decisiones (V(G) = 6, prueba CPC01).
     *
     * @param nueva     lesión a registrar
     * @param historial antecedentes obtenidos con {@link #buscarAntecedentes(Lesion)}
     * @return el antecedente que activa la alerta, o vacío si no hay alerta
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     */
    public Optional<Lesion> verificarRecurrencia(Lesion nueva, List<Lesion> historial)
            throws AccesoDenegadoException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        LocalDate limite = nueva.getFecha().minusMonths(MESES_VENTANA);
        for (Lesion anterior : historial) {                                            // 1
            if (!anterior.esMismaZona(nueva.getZona())) continue;                      // 2
            Tratamiento ultimo = anterior.getUltimoTratamiento();
            if (ultimo == null) return Optional.of(anterior);                          // 3
            if (ultimo.estaEnCurso()) return Optional.of(anterior);                    // 4
            if (!ultimo.getFechaFin().isBefore(limite)) return Optional.of(anterior);  // 5
        }
        return Optional.empty();
    }

    /**
     * Registra una lesión: vuelve a validarla y la persiste.
     *
     * @param lesion lesión nueva
     * @return el número de lesión asignado
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     * @throws ValidacionException     con todos los errores encontrados
     */
    public int registrarLesion(Lesion lesion)
            throws AccesoDenegadoException, ValidacionException, PersistenciaException {
        validar(lesion);
        return lesiones.insertar(lesion);
    }

    /**
     * Catálogo de zonas corporales.
     *
     * @return las zonas, en el orden del catálogo
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     */
    public List<ZonaCorporal> listarZonas() throws AccesoDenegadoException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        return zonas.listarTodos();
    }

    /**
     * Catálogo de tipos de lesión.
     *
     * @return los tipos, en el orden del catálogo
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     */
    public List<TipoLesion> listarTipos() throws AccesoDenegadoException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        return tipos.listarTodos();
    }

    /**
     * Fecha actual según el reloj del servicio.
     *
     * @return la fecha de hoy
     */
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }
}
