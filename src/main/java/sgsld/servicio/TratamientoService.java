package sgsld.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import sgsld.dao.EvolucionDAO;
import sgsld.dao.KinesiologoDAO;
import sgsld.dao.LesionDAO;
import sgsld.dao.TratamientoDAO;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Lesion;
import sgsld.modelo.Rol;
import sgsld.modelo.Tratamiento;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.Fechas;
import sgsld.util.PersistenciaException;
import sgsld.util.SesionUsuario;
import sgsld.util.Validaciones;
import sgsld.util.ValidacionException;

/**
 * Tratamientos y evoluciones (CU-05 y CU-06). Rol: Kinesiólogo.
 * <p>
 * Mantiene el grafo de objetos con los métodos del modelo
 * ({@code Lesion.agregarTratamiento}, {@code Tratamiento.agregarEvolucion},
 * {@code Tratamiento.finalizar}) y persiste cada cambio con los DAO, como en
 * el diagrama de secuencia de CU-06. Registrar la última evolución y finalizar
 * el tratamiento es una sola transacción
 * ({@link #registrarEvolucionYFinalizar(int, Evolucion, LocalDate)}).
 */
public class TratamientoService {

    private final TratamientoDAO tratamientos;
    private final LesionDAO lesiones;
    private final KinesiologoDAO kinesiologos;
    private final EvolucionDAO evoluciones;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param tratamientos DAO de tratamientos
     * @param lesiones     DAO de lesiones
     * @param kinesiologos DAO de kinesiólogos
     * @param evoluciones  DAO de evoluciones
     * @param reloj        reloj que define la fecha actual
     */
    public TratamientoService(TratamientoDAO tratamientos, LesionDAO lesiones, KinesiologoDAO kinesiologos,
                              EvolucionDAO evoluciones, Clock reloj) {
        this.tratamientos = tratamientos;
        this.lesiones = lesiones;
        this.kinesiologos = kinesiologos;
        this.evoluciones = evoluciones;
        this.reloj = reloj;
    }

    /**
     * Registra un tratamiento, que queda "En curso". Rechaza la operación si
     * la lesión ya tiene un tratamiento en curso (CU-05, 4a) o si la fecha de
     * inicio es anterior a la lesión o futura.
     *
     * @param tratamiento tratamiento nuevo
     * @return el número de tratamiento asignado
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     * @throws ValidacionException     con todos los errores encontrados
     */
    public int registrarTratamiento(Tratamiento tratamiento)
            throws AccesoDenegadoException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        List<String> errores = new ArrayList<>();
        Lesion lesion = tratamiento.getLesion();
        if (lesion == null || lesiones.buscarPorId(lesion.getId()).isEmpty()) {
            errores.add("Debe seleccionar una lesión registrada.");
        } else if (tratamientos.existeActivo(lesion.getId())) {
            errores.add(mensajeTratamientoEnCurso(lesion.getId()));
        }
        if (tratamiento.getKinesiologo() == null
                || kinesiologos.buscarPorId(tratamiento.getKinesiologo().getId()).isEmpty()) {
            errores.add("Debe seleccionar un kinesiólogo responsable registrado.");
        }
        LocalDate inicio = tratamiento.getFechaInicio();
        Validaciones.obligatorio(errores, inicio, "fecha de inicio");
        if (inicio != null && lesion != null && lesion.getFecha() != null && inicio.isBefore(lesion.getFecha())) {
            errores.add("La fecha de inicio no puede ser anterior a la fecha de la lesión ("
                    + Fechas.formatear(lesion.getFecha()) + ").");
        }
        if (inicio != null && inicio.isAfter(hoy())) {
            errores.add("La fecha de inicio no puede ser posterior a la fecha actual ("
                    + Fechas.formatear(hoy()) + ").");
        }
        Validaciones.texto(errores, tratamiento.getTipo(), "tipo de tratamiento", true, Tratamiento.MAX_TIPO);
        Validaciones.texto(errores, tratamiento.getObservaciones(), "observaciones", false,
                Tratamiento.MAX_OBSERVACIONES);
        ValidacionException.lanzarSiHayErrores(errores);
        int id;
        try {
            id = tratamientos.insertar(tratamiento);
        } catch (PersistenciaException e) {
            if (e.esClaveDuplicada()) {
                // Otro puesto registró un tratamiento en curso entre la validación y el guardado:
                // lo rechazó el índice único uq_tratamiento_en_curso.
                throw new ValidacionException(mensajeTratamientoEnCurso(lesion.getId()));
            }
            throw e;
        }
        lesion.agregarTratamiento(tratamiento);
        return id;
    }

    /**
     * Registra una evolución de un tratamiento en curso. La fecha debe estar
     * entre el inicio del tratamiento y hoy (CU-06, 3a).
     *
     * @param idTratamiento número de tratamiento
     * @param evolucion     evolución nueva
     * @return el número de evolución asignado
     * @throws AccesoDenegadoException      si el rol no es Kinesiólogo
     * @throws EntidadNoEncontradaException si el tratamiento no existe
     * @throws ValidacionException          con todos los errores encontrados
     */
    public int registrarEvolucion(int idTratamiento, Evolucion evolucion)
            throws AccesoDenegadoException, EntidadNoEncontradaException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        Tratamiento tratamiento = buscar(idTratamiento);
        List<String> errores = new ArrayList<>();
        validarEvolucion(tratamiento, evolucion, errores);
        ValidacionException.lanzarSiHayErrores(errores);
        tratamiento.agregarEvolucion(evolucion);
        return evoluciones.insertar(idTratamiento, evolucion);
    }

    /**
     * Registra la última evolución de un tratamiento y lo finaliza en <b>una
     * sola transacción</b>: se guardan las dos cosas o ninguna. Valida lo mismo
     * que {@link #registrarEvolucion(int, Evolucion)} y
     * {@link #finalizarTratamiento(int, LocalDate)}, y además que el fin no sea
     * anterior a la evolución.
     *
     * @param idTratamiento número de tratamiento en curso
     * @param evolucion     última evolución
     * @param fechaFin      fecha de fin (la interfaz propone la de la evolución)
     * @return el número de evolución asignado
     * @throws AccesoDenegadoException      si el rol no es Kinesiólogo
     * @throws EntidadNoEncontradaException si el tratamiento no existe
     * @throws ValidacionException          con todos los errores encontrados
     * @throws PersistenciaException        si falla el guardado (ya se deshizo todo)
     */
    public int registrarEvolucionYFinalizar(int idTratamiento, Evolucion evolucion, LocalDate fechaFin)
            throws AccesoDenegadoException, EntidadNoEncontradaException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        Tratamiento tratamiento = buscar(idTratamiento);
        List<String> errores = new ArrayList<>();
        validarEvolucion(tratamiento, evolucion, errores);
        validarFin(tratamiento, fechaFin, errores);
        if (fechaFin != null && evolucion.getFecha() != null && fechaFin.isBefore(evolucion.getFecha())) {
            errores.add("La fecha de fin no puede ser anterior a la fecha de la evolución ("
                    + Fechas.formatear(evolucion.getFecha()) + ").");
        }
        ValidacionException.lanzarSiHayErrores(errores);
        tratamiento.agregarEvolucion(evolucion);
        tratamiento.finalizar(fechaFin);
        return tratamientos.registrarEvolucionYFinalizar(idTratamiento, evolucion, fechaFin);
    }

    /**
     * Finaliza un tratamiento en curso. La fecha de fin debe estar entre el
     * inicio y hoy.
     * <p>
     * Devuelve el tratamiento finalizado: con MySQL cada lectura arma objetos
     * nuevos, de modo que la vista no debe suponer que el objeto que tenía es
     * el mismo que finalizó el servicio.
     *
     * @param idTratamiento número de tratamiento
     * @param fechaFin      fecha de fin
     * @return el tratamiento finalizado (con su duración)
     * @throws AccesoDenegadoException      si el rol no es Kinesiólogo
     * @throws EntidadNoEncontradaException si el tratamiento no existe
     * @throws ValidacionException          con todos los errores encontrados
     */
    public Tratamiento finalizarTratamiento(int idTratamiento, LocalDate fechaFin)
            throws AccesoDenegadoException, EntidadNoEncontradaException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        Tratamiento tratamiento = buscar(idTratamiento);
        List<String> errores = new ArrayList<>();
        if (!tratamiento.estaEnCurso()) {
            errores.add("El tratamiento N° " + idTratamiento + " ya está finalizado.");
        }
        validarFin(tratamiento, fechaFin, errores);
        ValidacionException.lanzarSiHayErrores(errores);
        tratamiento.finalizar(fechaFin);
        tratamientos.finalizar(idTratamiento, fechaFin);
        return tratamiento;
    }

    /**
     * Lista los tratamientos en curso, por número.
     *
     * @return los tratamientos en curso
     * @throws AccesoDenegadoException si el rol no es Kinesiólogo
     */
    public List<Tratamiento> listarEnCurso() throws AccesoDenegadoException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        return tratamientos.listarEnCurso();
    }

    /**
     * Busca un tratamiento por su número.
     *
     * @param idTratamiento número de tratamiento
     * @return el tratamiento
     * @throws AccesoDenegadoException      si el rol no es Kinesiólogo
     * @throws EntidadNoEncontradaException si no existe
     */
    public Tratamiento buscarPorId(int idTratamiento)
            throws AccesoDenegadoException, EntidadNoEncontradaException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.KINESIOLOGO);
        return buscar(idTratamiento);
    }

    /**
     * Fecha actual según el reloj del servicio.
     *
     * @return la fecha de hoy
     */
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private Tratamiento buscar(int idTratamiento) throws EntidadNoEncontradaException, PersistenciaException {
        return tratamientos.buscarPorId(idTratamiento).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró el tratamiento N° " + idTratamiento + "."));
    }

    private static String mensajeTratamientoEnCurso(int idLesion) {
        return "La lesión N° " + idLesion + " ya tiene un tratamiento en curso. "
                + "Debe finalizarlo antes de registrar uno nuevo.";
    }

    /** Tratamiento en curso; fecha entre el inicio y hoy (CU-06, 3a); observación y resultado obligatorios. */
    private void validarEvolucion(Tratamiento tratamiento, Evolucion evolucion, List<String> errores) {
        if (!tratamiento.estaEnCurso()) {
            errores.add("Solo se registran evoluciones en tratamientos en curso.");
        }
        Validaciones.obligatorio(errores, evolucion.getFecha(), "fecha");
        if (evolucion.getFecha() != null && fueraDeRango(evolucion.getFecha(), tratamiento)) {
            errores.add("La fecha de la evolución debe estar entre el inicio del tratamiento ("
                    + Fechas.formatear(tratamiento.getFechaInicio()) + ") y la fecha actual ("
                    + Fechas.formatear(hoy()) + ").");
        }
        Validaciones.texto(errores, evolucion.getObservacion(), "observación", true, Evolucion.MAX_OBSERVACION);
        Validaciones.obligatorio(errores, evolucion.getResultado(), "resultado");
    }

    /** Fecha de fin obligatoria, entre el inicio del tratamiento y hoy. */
    private void validarFin(Tratamiento tratamiento, LocalDate fechaFin, List<String> errores) {
        Validaciones.obligatorio(errores, fechaFin, "fecha de fin");
        if (fechaFin != null && fueraDeRango(fechaFin, tratamiento)) {
            errores.add("La fecha de fin debe estar entre el inicio del tratamiento ("
                    + Fechas.formatear(tratamiento.getFechaInicio()) + ") y la fecha actual ("
                    + Fechas.formatear(hoy()) + ").");
        }
    }

    /** Indica si la fecha queda fuera del intervalo [inicio del tratamiento, hoy]. */
    private boolean fueraDeRango(LocalDate fecha, Tratamiento tratamiento) {
        return fecha.isBefore(tratamiento.getFechaInicio()) || fecha.isAfter(hoy());
    }
}
