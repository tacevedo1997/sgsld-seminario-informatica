package sgsld.dao.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.EstadoTratamiento;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Region;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.Rol;

/** Piezas de {@link AccesoDatosMySQL} que no necesitan una base: parámetros, LIKE y enumeraciones. */
@DisplayName("AccesoDatosMySQL · parámetros, LIKE y ENUM (sin MySQL)")
class AccesoDatosMySQLTest {

    /** Llamadas que recibe la sentencia simulada, por ejemplo "setInt[2, 7]". */
    private final List<String> llamadas = new ArrayList<>();

    /** PreparedStatement simulado con un Proxy del JDK (sin Mockito): registra cada llamada. */
    private PreparedStatement sentenciaSimulada() {
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {PreparedStatement.class}, (proxy, metodo, argumentos) -> {
                    llamadas.add(metodo.getName() + Arrays.toString(argumentos));
                    return null;
                });
    }

    @Test
    @DisplayName("Cada tipo de parámetro va a su setter: LocalDate a Date, enumeraciones a su valor ENUM, null a setNull")
    void asignaSegunElTipo() throws Exception {
        AccesoDatosMySQL.asignarParametros(sentenciaSimulada(), "42587634", 7, 8L, true,
                LocalDate.of(2026, 9, 26), Gravedad.LEVE, Rol.KINESIOLOGO, EstadoTratamiento.EN_CURSO,
                Region.MIEMBRO_INFERIOR, null);
        assertEquals(List.of(
                "setString[1, 42587634]",
                "setInt[2, 7]",
                "setLong[3, 8]",
                "setBoolean[4, true]",
                "setDate[5, 2026-09-26]",
                "setString[6, Leve]",
                "setString[7, KINESIOLOGO]",
                "setString[8, En curso]",
                "setString[9, Miembro inferior]",
                "setNull[10, 0]"), llamadas);
    }

    @Test
    @DisplayName("Un tipo no previsto se rechaza en lugar de convertirse en texto")
    void tipoNoAdmitido() {
        assertThrows(IllegalArgumentException.class,
                () -> AccesoDatosMySQL.asignarParametros(sentenciaSimulada(), new Object()));
    }

    @Test
    @DisplayName("LIKE de coincidencia parcial: los comodines del usuario se escapan")
    void patronLike() {
        assertEquals("%gom%", AccesoDatosMySQL.patronContiene(" gom "));
        assertEquals("%50\\%\\_x\\\\%", AccesoDatosMySQL.patronContiene("50%_x\\"));
        assertEquals("%%", AccesoDatosMySQL.patronContiene(null));
    }

    @Test
    @DisplayName("Columnas ENUM a enumeraciones: nombre en Rol, etiqueta en las demás")
    void valoresEnum() {
        assertEquals(Rol.CUERPO_TECNICO, ValoresEnum.rol("CUERPO_TECNICO"));
        assertEquals(Gravedad.MODERADA, ValoresEnum.gravedad("Moderada"));
        assertEquals(EstadoTratamiento.FINALIZADO, ValoresEnum.estadoTratamiento("Finalizado"));
        assertEquals(ResultadoEvolucion.DESFAVORABLE, ValoresEnum.resultado("Desfavorable"));
        assertEquals(Region.CABEZA_Y_CUELLO, ValoresEnum.region("Cabeza y cuello"));
        assertEquals(EstadoDisponibilidad.EN_TRATAMIENTO, ValoresEnum.estadoDisponibilidad("En tratamiento"));
        assertThrows(IllegalArgumentException.class, () -> ValoresEnum.gravedad("Leve "));
    }

    @Test
    @DisplayName("Ninguna sentencia de la aplicación usa DELETE: las bajas son lógicas")
    void sinDelete() throws Exception {
        try (Stream<Path> fuentes = Files.walk(Path.of("src", "main", "java"))) {
            for (Path fuente : (Iterable<Path>) fuentes.filter(p -> p.toString().endsWith(".java"))::iterator) {
                String texto = Files.readString(fuente, StandardCharsets.UTF_8);
                assertFalse(texto.contains("\"DELETE") || texto.contains(" DELETE FROM"), "DELETE en " + fuente);
            }
        }
    }
}
