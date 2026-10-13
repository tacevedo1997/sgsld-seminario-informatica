package sgsld.dao.memoria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import sgsld.dao.JugadorDAO;
import sgsld.modelo.Jugador;
import sgsld.util.Algoritmos;
import sgsld.util.Texto;

/**
 * Jugadores en memoria.
 * <p>
 * Además de la tabla por id, mantiene un <b>índice ordenado por DNI</b>
 * (análogo al índice {@code UNIQUE uq_jugador_dni} de MySQL): cada alta se
 * inserta en la posición que indica la búsqueda binaria, y
 * {@link #buscarPorDni(String)} la usa para encontrar un jugador en O(log n).
 * La búsqueda por apellido, en cambio, es secuencial (O(n)), como un
 * {@code LIKE '%texto%'} que no aprovecha índices.
 */
public class JugadorDAOMemoria extends DAOMemoria<Jugador> implements JugadorDAO {

    private static final Comparator<String> ORDEN_DNI = Comparator.naturalOrder();

    private final List<Jugador> indicePorDni = new ArrayList<>();

    /**
     * {@inheritDoc}
     * <p>
     * Ubica al jugador en el índice por DNI con búsqueda binaria.
     *
     * @throws IllegalArgumentException si el DNI ya existe (como el error 1062 de MySQL)
     */
    @Override
    public int insertar(Jugador jugador) {
        Objects.requireNonNull(jugador, "jugador");
        int posicion = buscarEnIndice(jugador.getDni());
        if (posicion >= 0) {
            throw new IllegalArgumentException("DNI duplicado: " + jugador.getDni());
        }
        int id = super.insertar(jugador);
        indicePorDni.add(-posicion - 1, jugador);
        return id;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Si el DNI cambió y el jugador quedó fuera de orden en el índice, lo
     * reubica en la posición que indica la búsqueda binaria.
     *
     * @throws IllegalArgumentException si el nuevo DNI pertenece a otro jugador
     */
    @Override
    public void actualizar(Jugador jugador) {
        Objects.requireNonNull(jugador, "jugador");
        boolean duplicado = buscarPrimero(otro -> otro.getId() != jugador.getId()
                && Objects.equals(otro.getDni(), jugador.getDni())).isPresent();
        if (duplicado) {
            throw new IllegalArgumentException("DNI duplicado: " + jugador.getDni());
        }
        super.actualizar(jugador);
        reubicarEnIndice(jugador);
    }

    /** Búsqueda binaria en el índice ordenado por DNI: O(log n). */
    @Override
    public Optional<Jugador> buscarPorDni(String dni) {
        if (dni == null) {
            return Optional.empty();
        }
        int posicion = buscarEnIndice(dni.trim());
        return posicion >= 0 ? Optional.of(indicePorDni.get(posicion)) : Optional.empty();
    }

    /** Búsqueda secuencial sobre el apellido normalizado: O(n). */
    @Override
    public List<Jugador> buscarPorApellido(String apellido) {
        return filtrar(jugador -> Texto.contiene(jugador.getApellido(), apellido));
    }

    @Override
    public List<Jugador> listarActivos() {
        return filtrar(Jugador::isActivo);
    }

    private int buscarEnIndice(String dni) {
        Objects.requireNonNull(dni, "dni");
        return Algoritmos.busquedaBinaria(indicePorDni, dni, Jugador::getDni, ORDEN_DNI);
    }

    private void reubicarEnIndice(Jugador jugador) {
        int actual = -1;
        for (int i = 0; i < indicePorDni.size() && actual < 0; i++) {
            if (indicePorDni.get(i).getId() == jugador.getId()) {
                actual = i;
            }
        }
        if (actual >= 0 && indicePorDni.get(actual) == jugador && estaEnOrden(actual)) {
            return;
        }
        if (actual >= 0) {
            indicePorDni.remove(actual);
        }
        indicePorDni.add(-buscarEnIndice(jugador.getDni()) - 1, jugador);
    }

    private boolean estaEnOrden(int posicion) {
        String dni = indicePorDni.get(posicion).getDni();
        boolean despuesDelAnterior = posicion == 0
                || ORDEN_DNI.compare(indicePorDni.get(posicion - 1).getDni(), dni) < 0;
        boolean antesDelSiguiente = posicion == indicePorDni.size() - 1
                || ORDEN_DNI.compare(dni, indicePorDni.get(posicion + 1).getDni()) < 0;
        return despuesDelAnterior && antesDelSiguiente;
    }
}
