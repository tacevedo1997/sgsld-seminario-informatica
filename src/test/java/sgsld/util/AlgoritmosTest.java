package sgsld.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas de {@link Algoritmos}: ordenamiento por inserción y búsqueda binaria. */
@DisplayName("Algoritmos · ordenamiento por inserción y búsqueda binaria")
class AlgoritmosTest {

    /** Elemento con clave repetible y etiqueta que revela el orden original. */
    private record Elemento(int clave, String etiqueta) {
    }

    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();

    private static List<Integer> lista(Integer... valores) {
        return new ArrayList<>(List.of(valores));
    }

    @Test
    @DisplayName("Ordenamiento estable · los iguales conservan su orden relativo")
    void ordenamientoEstable() {
        List<Elemento> elementos = new ArrayList<>(List.of(
                new Elemento(3, "a"), new Elemento(1, "b"), new Elemento(3, "c"),
                new Elemento(2, "d"), new Elemento(1, "e"), new Elemento(3, "f")));
        Algoritmos.ordenarPorInsercion(elementos, Comparator.comparingInt(Elemento::clave));
        List<String> etiquetas = new ArrayList<>();
        for (Elemento e : elementos) {
            etiquetas.add(e.etiqueta());
        }
        assertEquals(List.of("b", "e", "d", "a", "c", "f"), etiquetas);
    }

    @Test
    @DisplayName("Ordenamiento · lista vacía y de un elemento")
    void listaVaciaYUnitaria() {
        List<Integer> vacia = lista();
        Algoritmos.ordenarPorInsercion(vacia, NATURAL);
        assertTrue(vacia.isEmpty());
        List<Integer> unitaria = lista(7);
        Algoritmos.ordenarPorInsercion(unitaria, NATURAL);
        assertEquals(List.of(7), unitaria);
    }

    @Test
    @DisplayName("Ordenamiento · lista ya ordenada")
    void listaOrdenada() {
        List<Integer> valores = lista(1, 2, 3, 4, 5);
        Algoritmos.ordenarPorInsercion(valores, NATURAL);
        assertEquals(List.of(1, 2, 3, 4, 5), valores);
    }

    @Test
    @DisplayName("Ordenamiento · lista invertida (peor caso)")
    void listaInvertida() {
        List<Integer> valores = lista(5, 4, 3, 2, 1);
        Algoritmos.ordenarPorInsercion(valores, NATURAL);
        assertEquals(List.of(1, 2, 3, 4, 5), valores);
    }

    @Test
    @DisplayName("Ordenamiento · criterio descendente y alfabético con tildes")
    void criteriosCompuestos() {
        List<Integer> valores = lista(2, 9, 4);
        Algoritmos.ordenarPorInsercion(valores, NATURAL.reversed());
        assertEquals(List.of(9, 4, 2), valores);
        List<String> apellidos = new ArrayList<>(List.of("Pérez", "Acosta", "Díaz", "Castro"));
        Algoritmos.ordenarPorInsercion(apellidos, Texto.ordenAlfabetico());
        assertEquals(List.of("Acosta", "Castro", "Díaz", "Pérez"), apellidos);
    }

    @Test
    @DisplayName("Búsqueda binaria · clave encontrada")
    void busquedaEncontrada() {
        List<Integer> valores = lista(10, 20, 30, 40, 50);
        assertEquals(0, Algoritmos.busquedaBinaria(valores, 10, Function.identity(), NATURAL));
        assertEquals(2, Algoritmos.busquedaBinaria(valores, 30, Function.identity(), NATURAL));
        assertEquals(4, Algoritmos.busquedaBinaria(valores, 50, Function.identity(), NATURAL));
    }

    @Test
    @DisplayName("Búsqueda binaria · clave no encontrada y punto de inserción")
    void busquedaNoEncontrada() {
        List<Integer> valores = lista(10, 20, 30);
        assertEquals(-1, Algoritmos.busquedaBinaria(valores, 5, Function.identity(), NATURAL));
        assertEquals(-3, Algoritmos.busquedaBinaria(valores, 25, Function.identity(), NATURAL));
        assertEquals(-4, Algoritmos.busquedaBinaria(valores, 35, Function.identity(), NATURAL));
        assertEquals(-1, Algoritmos.busquedaBinaria(lista(), 1, Function.identity(), NATURAL));
        int resultado = Algoritmos.busquedaBinaria(valores, 25, Function.identity(), NATURAL);
        assertEquals(2, -resultado - 1);
    }

    @Test
    @DisplayName("Búsqueda binaria · por clave extraída (DNI como texto)")
    void busquedaPorClaveExtraida() {
        List<Elemento> elementos = List.of(
                new Elemento(1, "39874521"), new Elemento(2, "41236587"), new Elemento(3, "42587634"));
        int indice = Algoritmos.busquedaBinaria(elementos, "41236587", Elemento::etiqueta,
                Comparator.<String>naturalOrder());
        assertEquals(1, indice);
    }
}
