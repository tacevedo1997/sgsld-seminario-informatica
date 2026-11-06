package sgsld.dao.memoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.modelo.Categoria;
import sgsld.modelo.Jugador;

/** Pruebas del índice por DNI (búsqueda binaria) y de la búsqueda por apellido. */
@DisplayName("JugadorDAOMemoria · índice ordenado por DNI")
class JugadorDAOMemoriaTest {

    private static final List<String> DNIS_DEL_LOTE = List.of(
            "41236587", "42587634", "45120398", "49365120", "50214786", "46078912", "51347025", "39874521");

    private FabricaDAOMemoria fabrica;
    private JugadorDAOMemoria jugadores;

    @BeforeEach
    void cargar() {
        fabrica = new FabricaDAOMemoria();
        DatosDePrueba.cargar(fabrica);
        jugadores = fabrica.getJugadorDAO();
    }

    @Test
    @DisplayName("Encuentra cada DNI del lote y no encuentra uno inexistente")
    void buscaPorDni() {
        for (String dni : DNIS_DEL_LOTE) {
            assertEquals(dni, jugadores.buscarPorDni(dni).orElseThrow().getDni());
        }
        assertTrue(jugadores.buscarPorDni("12345678").isEmpty());
    }

    @Test
    @DisplayName("El alta se ubica en el índice y un DNI repetido se rechaza")
    void altaEnIndice() {
        Categoria primera = fabrica.getCategoriaDAO().buscarPorId(1).orElseThrow();
        Jugador nuevo = new Jugador("40000000", "Martín", "Suárez", LocalDate.of(2001, 5, 10), null,
                "Defensor", primera);
        assertEquals(9, jugadores.insertar(nuevo));
        assertEquals(nuevo, jugadores.buscarPorDni("40000000").orElseThrow());
        Jugador duplicado = new Jugador("42587634", "Otro", "Jugador", LocalDate.of(2001, 5, 10), null,
                "Defensor", primera);
        assertThrows(IllegalArgumentException.class, () -> jugadores.insertar(duplicado));
    }

    @Test
    @DisplayName("Al modificar el DNI, el jugador se reubica en el índice")
    void reubicaAlModificarDni() {
        Jugador molina = jugadores.buscarPorDni("39874521").orElseThrow();
        molina.setDni("99999999");
        jugadores.actualizar(molina);
        assertTrue(jugadores.buscarPorDni("39874521").isEmpty());
        assertEquals(molina, jugadores.buscarPorDni("99999999").orElseThrow());
        for (String dni : DNIS_DEL_LOTE.subList(0, 7)) {
            assertTrue(jugadores.buscarPorDni(dni).isPresent(), "Sigue encontrando " + dni);
        }
    }

    @Test
    @DisplayName("La búsqueda por apellido es parcial y no distingue mayúsculas ni tildes")
    void buscaPorApellido() {
        List<Jugador> resultado = jugadores.buscarPorApellido("PEREZ");
        assertEquals(1, resultado.size());
        assertEquals("Pérez", resultado.get(0).getApellido());
        assertEquals(2, jugadores.buscarPorApellido("ro").size(), "Romero y Castro");
    }
}
