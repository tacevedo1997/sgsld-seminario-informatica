package sgsld.dao.memoria;

import java.time.LocalDate;
import java.util.List;

import sgsld.dao.TratamientoDAO;
import sgsld.modelo.Tratamiento;

/** Tratamientos en memoria. */
public class TratamientoDAOMemoria extends DAOMemoria<Tratamiento> implements TratamientoDAO {

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
     * equivaldrá al {@code UPDATE}).
     *
     * @throws IllegalArgumentException si el tratamiento no existe
     * @throws IllegalStateException    si ya estaba finalizado con otra fecha
     */
    @Override
    public void finalizar(int idTratamiento, LocalDate fechaFin) {
        Tratamiento tratamiento = buscarPorId(idTratamiento).orElseThrow(
                () -> new IllegalArgumentException("No existe el tratamiento " + idTratamiento));
        if (tratamiento.estaEnCurso()) {
            tratamiento.finalizar(fechaFin);
        } else if (!tratamiento.getFechaFin().equals(fechaFin)) {
            throw new IllegalStateException("El tratamiento " + idTratamiento + " ya estaba finalizado");
        }
    }

    @Override
    public List<Tratamiento> listarEnCurso() {
        return filtrar(Tratamiento::estaEnCurso);
    }
}
