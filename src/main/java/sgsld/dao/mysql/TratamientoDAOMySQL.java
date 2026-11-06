package sgsld.dao.mysql;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import sgsld.dao.TratamientoDAO;
import sgsld.modelo.EstadoTratamiento;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Tratamiento;
import sgsld.util.PersistenciaException;
import sgsld.util.PersistenciaException.Motivo;

/**
 * Tratamientos en MySQL (tabla {@code tratamiento}).
 * <p>
 * El índice único {@code uq_tratamiento_en_curso} es la garantía final de un
 * único tratamiento en curso por lesión, aun con dos puestos a la vez. Las
 * lecturas devuelven cada tratamiento dentro de su grafo (lesión, jugador,
 * kinesiólogo y evoluciones).
 */
public class TratamientoDAOMySQL extends AccesoDatosMySQL implements TratamientoDAO {

    private static final String INSERTAR = "INSERT INTO tratamiento (id_lesion, id_kinesiologo, fecha_inicio, "
            + "tipo, observaciones) VALUES (?, ?, ?, ?, ?)";
    private static final String ACTUALIZAR = "UPDATE tratamiento SET id_kinesiologo = ?, tipo = ?, "
            + "observaciones = ? WHERE id_tratamiento = ?";
    private static final String FINALIZAR = "UPDATE tratamiento SET estado = ?, fecha_fin = ? "
            + "WHERE id_tratamiento = ? AND estado = ?";
    private static final String EXISTE_ACTIVO =
            "SELECT EXISTS (SELECT 1 FROM tratamiento WHERE id_lesion = ? AND estado = ?)";
    private static final String DE_LA_LESION_DEL_TRATAMIENTO =
            "l.id_lesion = (SELECT t2.id_lesion FROM tratamiento t2 WHERE t2.id_tratamiento = ?)";
    private static final String CON_TRATAMIENTO_EN_ESTADO =
            "l.id_lesion IN (SELECT t2.id_lesion FROM tratamiento t2 WHERE t2.estado = ?)";

    private final CargadorGrafo grafo;

    /**
     * Crea el DAO.
     *
     * @param conexion conexión a la base
     */
    public TratamientoDAOMySQL(ConexionBD conexion) {
        super(conexion);
        this.grafo = new CargadorGrafo(conexion);
    }

    /** El estado inicial ("En curso") lo pone el valor por defecto de la columna. */
    @Override
    public int insertar(Tratamiento tratamiento) throws PersistenciaException {
        exigirNueva(tratamiento);
        tratamiento.setId(insertar(INSERTAR, tratamiento.getLesion().getId(), tratamiento.getKinesiologo().getId(),
                tratamiento.getFechaInicio(), tratamiento.getTipo(), tratamiento.getObservaciones()));
        return tratamiento.getId();
    }

    /** El estado y la fecha de fin solo cambian con {@link #finalizar(int, LocalDate)}. */
    @Override
    public void actualizar(Tratamiento tratamiento) throws PersistenciaException {
        exigirExistente(actualizar(ACTUALIZAR, tratamiento.getKinesiologo().getId(), tratamiento.getTipo(),
                tratamiento.getObservaciones(), tratamiento.getId()), tratamiento);
    }

    @Override
    public Optional<Tratamiento> buscarPorId(int id) throws PersistenciaException {
        for (Tratamiento tratamiento : CargadorGrafo.tratamientos(grafo.cargar(true, DE_LA_LESION_DEL_TRATAMIENTO, id))) {
            if (tratamiento.getId() == id) {
                return Optional.of(tratamiento);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Tratamiento> listarTodos() throws PersistenciaException {
        return CargadorGrafo.tratamientos(grafo.cargar(true, "TRUE"));
    }

    @Override
    public boolean existeActivo(int idLesion) throws PersistenciaException {
        return consultarUno(EXISTE_ACTIVO, fila -> fila.getBoolean(1), idLesion, EstadoTratamiento.EN_CURSO)
                .orElse(false);
    }

    /**
     * {@inheritDoc}
     *
     * @throws PersistenciaException con motivo {@code CONFLICTO} si el tratamiento
     *         ya no estaba en curso (por ejemplo, otro puesto lo finalizó)
     */
    @Override
    public void finalizar(int idTratamiento, LocalDate fechaFin) throws PersistenciaException {
        if (actualizar(FINALIZAR, EstadoTratamiento.FINALIZADO, fechaFin, idTratamiento,
                EstadoTratamiento.EN_CURSO) == 0) {
            throw yaNoEstaEnCurso(idTratamiento);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Inserta la evolución y finaliza el tratamiento con la misma conexión: si
     * el {@code UPDATE} falla (por ejemplo, la restricción
     * {@code ck_tratamiento_fechas}) o el tratamiento ya no está en curso, el
     * {@code rollback} deshace también la evolución.
     */
    @Override
    public int registrarEvolucionYFinalizar(int idTratamiento, Evolucion evolucion, LocalDate fechaFin)
            throws PersistenciaException {
        exigirNueva(evolucion);
        int idEvolucion = enTransaccion("registrar la evolución y finalizar el tratamiento", conexion -> {
            int id = insertar(conexion, EvolucionDAOMySQL.INSERTAR, idTratamiento, evolucion.getFecha(),
                    evolucion.getObservacion(), evolucion.getResultado());
            if (actualizar(conexion, FINALIZAR, EstadoTratamiento.FINALIZADO, fechaFin, idTratamiento,
                    EstadoTratamiento.EN_CURSO) == 0) {
                throw yaNoEstaEnCurso(idTratamiento);
            }
            return id;
        });
        evolucion.setId(idEvolucion);
        return idEvolucion;
    }

    @Override
    public List<Tratamiento> listarEnCurso() throws PersistenciaException {
        List<Tratamiento> enCurso = new ArrayList<>();
        for (Tratamiento tratamiento : CargadorGrafo.tratamientos(
                grafo.cargar(true, CON_TRATAMIENTO_EN_ESTADO, EstadoTratamiento.EN_CURSO))) {
            if (tratamiento.estaEnCurso()) {
                enCurso.add(tratamiento);
            }
        }
        return enCurso;
    }

    private static PersistenciaException yaNoEstaEnCurso(int idTratamiento) {
        return new PersistenciaException(Motivo.CONFLICTO, "El tratamiento N° " + idTratamiento
                + " ya no está en curso: otro usuario pudo haberlo finalizado. Actualice los datos.", null);
    }
}
