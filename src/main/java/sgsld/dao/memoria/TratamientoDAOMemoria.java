package sgsld.dao.memoria;

import java.time.LocalDate;
import java.util.List;

import sgsld.dao.TratamientoDAO;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Tratamiento;

/** Tratamientos en memoria. */
public class TratamientoDAOMemoria extends DAOMemoria<Tratamiento> implements TratamientoDAO {

    private final EvolucionDAOMemoria evoluciones;

    /**
     * Crea el DAO, que guarda las evoluciones en la tabla de evoluciones en memoria.
     *
     * @param evoluciones DAO de evoluciones en memoria
     */
    public TratamientoDAOMemoria(EvolucionDAOMemoria evoluciones) {
        this.evoluciones = evoluciones;
    }

    @Override
    public boolean existeActivo(int idLesion) {
        return buscarPrimero(tratamiento -> tratamiento.estaEnCurso()
                && tratamiento.getLesion().getId() == idLesion).isPresent();
    }

    /**
     * {@inheritDoc}
     * <p>
     * En memoria, la instancia guardada suele ser la misma que el servicio ya
     * finalizó con {@link Tratamiento#finalizar(LocalDate)}; en ese caso solo
     * se verifica la coherencia. Si sigue en curso, se finaliza aquí (en MySQL
     * equivale al {@code UPDATE}).
     *
     * @throws IllegalArgumentException si el tratamiento no existe
     * @throws IllegalStateException    si ya estaba finalizado con otra fecha
     */
    @Override
    public void finalizar(int idTratamiento, LocalDate fechaFin) {
        Tratamiento tratamiento = buscar(idTratamiento);
        if (tratamiento.estaEnCurso()) {
            tratamiento.finalizar(fechaFin);
        } else if (!tratamiento.getFechaFin().equals(fechaFin)) {
            throw new IllegalStateException("El tratamiento " + idTratamiento + " ya estaba finalizado");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * En memoria no hay transacciones: el servicio ya validó los datos. Como en
     * {@link #finalizar(int, LocalDate)}, si el servicio ya actualizó la
     * instancia guardada solo se verifica la coherencia.
     *
     * @throws IllegalStateException si el tratamiento quedó finalizado de otro modo
     */
    @Override
    public int registrarEvolucionYFinalizar(int idTratamiento, Evolucion evolucion, LocalDate fechaFin) {
        Tratamiento tratamiento = buscar(idTratamiento);
        if (tratamiento.estaEnCurso()) {
            tratamiento.agregarEvolucion(evolucion);
            tratamiento.finalizar(fechaFin);
        } else if (!tratamiento.getEvoluciones().contains(evolucion) || !fechaFin.equals(tratamiento.getFechaFin())) {
            throw new IllegalStateException("El tratamiento " + idTratamiento + " ya estaba finalizado");
        }
        return evoluciones.insertar(idTratamiento, evolucion);
    }

    @Override
    public List<Tratamiento> listarEnCurso() {
        return filtrar(Tratamiento::estaEnCurso);
    }

    private Tratamiento buscar(int idTratamiento) {
        return buscarPorId(idTratamiento).orElseThrow(
                () -> new IllegalArgumentException("No existe el tratamiento " + idTratamiento));
    }
}
