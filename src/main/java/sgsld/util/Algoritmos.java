package sgsld.util;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Algoritmos de ordenación y búsqueda implementados a mano.
 * <p>
 * En el código de producción todo ordenamiento usa
 * {@link #ordenarPorInsercion(List, Comparator)} y toda búsqueda por clave en
 * una lista ordenada usa {@link #busquedaBinaria(List, Object, Function, Comparator)}.
 * La búsqueda por apellido es secuencial a propósito, para contrastar ambos
 * enfoques.
 */
public final class Algoritmos {

    private Algoritmos() {
    }

    /**
     * Ordena la lista en el lugar mediante ordenamiento por inserción.
     * <p>
     * Es <b>estable</b>: los elementos que el criterio considera iguales
     * conservan su orden relativo, porque un elemento solo se desplaza ante
     * otro estrictamente mayor.
     * <p>
     * Complejidad: O(n²) comparaciones en el peor caso (lista invertida) y O(n)
     * con datos ya ordenados o casi ordenados. Se eligió porque las listas que
     * ordena el sistema son chicas (historial de un jugador, filas de un
     * reporte, plantel de una categoría) y suelen llegar casi ordenadas, ya que
     * los registros se cargan en orden cronológico. Pensado para listas de
     * acceso aleatorio, como {@code ArrayList}.
     *
     * @param lista    lista modificable a ordenar
     * @param criterio criterio de comparación
     * @param <T>      tipo de los elementos
     */
    public static <T> void ordenarPorInsercion(List<T> lista, Comparator<? super T> criterio) {
        for (int i = 1; i < lista.size(); i++) {
            T actual = lista.get(i);
            int j = i - 1;
            while (j >= 0 && criterio.compare(lista.get(j), actual) > 0) {
                lista.set(j + 1, lista.get(j));
                j--;
            }
            lista.set(j + 1, actual);
        }
    }

    /**
     * Busca una clave en una lista ordenada mediante búsqueda binaria.
     * <p>
     * Precondición: la lista debe estar ordenada de forma ascendente según
     * {@code criterio} aplicado a la clave que devuelve {@code extractor}; si no
     * lo está, el resultado es indefinido.
     * <p>
     * Complejidad: O(log n) comparaciones, porque cada paso descarta la mitad
     * del intervalo restante.
     *
     * @param lista     lista ordenada por la clave
     * @param clave     clave buscada
     * @param extractor función que obtiene la clave de cada elemento
     * @param criterio  criterio de comparación de las claves
     * @param <T>       tipo de los elementos
     * @param <K>       tipo de la clave
     * @return el índice del elemento si se encuentra; si no,
     *         {@code -(punto de inserción) - 1}, como {@code Arrays.binarySearch}
     */
    public static <T, K> int busquedaBinaria(List<T> lista, K clave, Function<T, K> extractor,
                                             Comparator<? super K> criterio) {
        int inicio = 0;
        int fin = lista.size() - 1;
        while (inicio <= fin) {
            int medio = (inicio + fin) >>> 1;
            int comparacion = criterio.compare(extractor.apply(lista.get(medio)), clave);
            if (comparacion < 0) {
                inicio = medio + 1;
            } else if (comparacion > 0) {
                fin = medio - 1;
            } else {
                return medio;
            }
        }
        return -(inicio + 1);
    }
}
