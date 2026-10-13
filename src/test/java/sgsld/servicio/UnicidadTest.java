package sgsld.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sgsld.EscenarioPrueba;
import sgsld.modelo.Categoria;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.util.ValidacionException;

/** CPC06 · Unicidad de DNI, matrícula y nombre de categoría (como los índices UNIQUE de la base). */
@DisplayName("CPC06 · Unicidad")
class UnicidadTest {

    private final EscenarioPrueba escenario = new EscenarioPrueba().comoCoordinador();

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    @Test
    @DisplayName("CPC06-1 · DNI 42587634 duplicado: rechazo")
    void dniDuplicado() throws Exception {
        Categoria primera = escenario.categorias().buscarPorId(1);
        Jugador suarez = new Jugador("42587634", "Martín", "Suárez", LocalDate.of(2001, 5, 10), null,
                "Defensor", primera);
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.jugadores().registrar(suarez));
        assertTrue(error.getErrores().contains("Ya existe un jugador con DNI 42587634."));
        assertEquals(8, escenario.jugadores().listar(true).size());
    }

    @Test
    @DisplayName("CPC06-2 · Matrícula MP-10234 duplicada: rechazo")
    void matriculaDuplicada() throws Exception {
        Kinesiologo nuevo = new Kinesiologo("Ana", "Ríos", "MP-10234", null);
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.kinesiologos().registrar(nuevo));
        assertTrue(error.getErrores().contains("Ya existe un kinesiólogo con matrícula MP-10234."));
        assertEquals(3, escenario.kinesiologos().listar().size());
    }

    @ParameterizedTest(name = "CPC06-3 · Categoría \"{0}\" duplicada: rechazo")
    @DisplayName("CPC06-3 · Categoría Primera duplicada, sin distinguir mayúsculas")
    @ValueSource(strings = {"Primera", "primera", "PRIMERA"})
    void categoriaDuplicada(String nombre) throws Exception {
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> escenario.categorias().registrar(new Categoria(nombre, 18, 40)));
        assertTrue(error.getErrores().contains("Ya existe una categoría con el nombre Primera."));
        assertEquals(4, escenario.categorias().listar().size());
    }

    @Test
    @DisplayName("CPC06-4 · Modificar un jugador conservando su propio DNI no es duplicado")
    void modificarConservandoDni() throws Exception {
        Jugador gomez = escenario.jugadores().buscarPorDni("42587634");
        Jugador cambios = new Jugador(gomez.getId(), gomez.getDni(), gomez.getNombre(), gomez.getApellido(),
                gomez.getFechaNacimiento(), 8, gomez.getPosicion(), true, gomez.getCategoria());
        escenario.jugadores().modificar(cambios);
        assertEquals(8, escenario.jugadores().buscarPorDni("42587634").getNumeroCamiseta());
    }
}
