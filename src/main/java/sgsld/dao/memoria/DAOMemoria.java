package sgsld.dao.memoria;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import sgsld.dao.DAO;
import sgsld.modelo.Entidad;

/**
 * Implementación genérica de {@link DAO} en memoria: una "tabla"
 * {@code LinkedHashMap} indexada por id (conserva el orden de inserción) y un
 * contador autoincremental, como {@code AUTO_INCREMENT} en MySQL.
 * <p>
 * Guarda las mismas instancias que recibe: los cambios que los servicios hacen
 * sobre un objeto obtenido de aquí se ven en las demás referencias, como
 * ocurriría con una fila de la base.
 *
 * @param <T> tipo de entidad
 */
public abstract class DAOMemoria<T extends Entidad> implements DAO<T> {

    private final Map<Integer, T> tabla = new LinkedHashMap<>();
    private int contador;

    /**
     * {@inheritDoc}
     * <p>
     * Si la entidad trae id 0, le asigna el siguiente valor del contador. Si
     * ya trae un id (carga de datos), lo respeta y ajusta el contador.
     *
     * @throws IllegalArgumentException si ya existe un registro con ese id
     */
    @Override
    public int insertar(T entidad) {
        Objects.requireNonNull(entidad, "entidad");
        if (entidad.getId() == 0) {
            entidad.setId(contador + 1);
        } else if (tabla.containsKey(entidad.getId())) {
            throw new IllegalArgumentException("Ya existe un registro con id " + entidad.getId());
        }
        contador = Math.max(contador, entidad.getId());
        tabla.put(entidad.getId(), entidad);
        return entidad.getId();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException si no existe un registro con ese id
     */
    @Override
    public void actualizar(T entidad) {
        Objects.requireNonNull(entidad, "entidad");
        if (!tabla.containsKey(entidad.getId())) {
            throw new IllegalArgumentException("No existe un registro con id " + entidad.getId());
        }
        tabla.put(entidad.getId(), entidad);
    }

    @Override
    public Optional<T> buscarPorId(int id) {
        return Optional.ofNullable(tabla.get(id));
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(tabla.values());
    }

    /**
     * Recorre la tabla secuencialmente y devuelve los registros que cumplen la
     * condición (equivale a un {@code SELECT ... WHERE} sin índice).
     *
     * @param condicion condición a cumplir
     * @return los registros que la cumplen, en orden de inserción
     */
    protected List<T> filtrar(Predicate<? super T> condicion) {
        List<T> resultado = new ArrayList<>();
        for (T entidad : tabla.values()) {
            if (condicion.test(entidad)) {
                resultado.add(entidad);
            }
        }
        return resultado;
    }

    /**
     * Devuelve el primer registro que cumple la condición (búsqueda secuencial).
     *
     * @param condicion condición a cumplir
     * @return el registro, o vacío si ninguno la cumple
     */
    protected Optional<T> buscarPrimero(Predicate<? super T> condicion) {
        for (T entidad : tabla.values()) {
            if (condicion.test(entidad)) {
                return Optional.of(entidad);
            }
        }
        return Optional.empty();
    }
}
