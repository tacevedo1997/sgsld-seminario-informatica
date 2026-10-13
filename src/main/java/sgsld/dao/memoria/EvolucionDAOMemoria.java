package sgsld.dao.memoria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sgsld.dao.EvolucionDAO;
import sgsld.modelo.Evolucion;

/**
 * Evoluciones en memoria. Como {@link EvolucionDAO} no extiende {@code DAO<T>},
 * reutiliza {@link DAOMemoria} por composición (para la tabla y el contador) y
 * guarda aparte la clave foránea evolución → tratamiento.
 */
public class EvolucionDAOMemoria implements EvolucionDAO {

    private final DAOMemoria<Evolucion> tabla = new DAOMemoria<>() {
    };
    private final Map<Integer, Integer> idTratamientoPorEvolucion = new HashMap<>();

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException si el id de tratamiento no es válido
     */
    @Override
    public int insertar(int idTratamiento, Evolucion evolucion) {
        if (idTratamiento <= 0) {
            throw new IllegalArgumentException("Identificador de tratamiento inválido: " + idTratamiento);
        }
        int id = tabla.insertar(evolucion);
        idTratamientoPorEvolucion.put(id, idTratamiento);
        return id;
    }

    @Override
    public List<Evolucion> buscarPorTratamiento(int idTratamiento) {
        List<Evolucion> resultado = new ArrayList<>();
        for (Evolucion evolucion : tabla.listarTodos()) {
            if (idTratamientoPorEvolucion.get(evolucion.getId()) == idTratamiento) {
                resultado.add(evolucion);
            }
        }
        return resultado;
    }
}
