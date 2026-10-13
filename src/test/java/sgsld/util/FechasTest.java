package sgsld.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas de {@link Fechas}: formato estricto y descripción de períodos. */
@DisplayName("Fechas · formato dd/MM/aaaa y descripción de períodos")
class FechasTest {

    @Test
    @DisplayName("Figura 11 · 20/02/2026 a 15/06/2026 = \"3 meses y 26 días\"")
    void describePeriodoDeLaFigura11() {
        assertEquals("3 meses y 26 días",
                Fechas.describirPeriodo(LocalDate.of(2026, 2, 20), LocalDate.of(2026, 6, 15)));
    }

    @Test
    @DisplayName("Singular · \"1 mes y 1 día\", \"1 mes\" y \"1 día\"")
    void usaSingular() {
        assertEquals("1 mes y 1 día",
                Fechas.describirPeriodo(LocalDate.of(2026, 2, 20), LocalDate.of(2026, 3, 21)));
        assertEquals("1 mes",
                Fechas.describirPeriodo(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15)));
        assertEquals("1 día",
                Fechas.describirPeriodo(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26)));
    }

    @Test
    @DisplayName("Solo días · \"26 días\"")
    void soloDias() {
        assertEquals("26 días",
                Fechas.describirPeriodo(LocalDate.of(2026, 5, 20), LocalDate.of(2026, 6, 15)));
    }

    @Test
    @DisplayName("Solo meses · \"2 meses\" y, sin años, \"14 meses\"")
    void soloMeses() {
        assertEquals("2 meses",
                Fechas.describirPeriodo(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 10)));
        assertEquals("14 meses",
                Fechas.describirPeriodo(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 3, 1)));
    }

    @Test
    @DisplayName("Misma fecha · \"0 días\"")
    void mismaFecha() {
        LocalDate dia = LocalDate.of(2026, 9, 26);
        assertEquals("0 días", Fechas.describirPeriodo(dia, dia));
    }

    @Test
    @DisplayName("Período invertido · IllegalArgumentException")
    void periodoInvertido() {
        assertThrows(IllegalArgumentException.class,
                () -> Fechas.describirPeriodo(LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 25)));
    }

    @Test
    @DisplayName("parsear · acepta 26/09/2026 y formatear lo reconstruye")
    void parseaYFormatea() {
        LocalDate fecha = Fechas.parsear(" 26/09/2026 ");
        assertEquals(LocalDate.of(2026, 9, 26), fecha);
        assertEquals("26/09/2026", Fechas.formatear(fecha));
        assertEquals("-", Fechas.formatear(null));
    }

    @Test
    @DisplayName("parsear · rechaza la fecha inexistente 31/02/2026")
    void rechazaFechaInexistente() {
        assertThrows(DateTimeParseException.class, () -> Fechas.parsear("31/02/2026"));
    }

    @Test
    @DisplayName("parsear · rechaza el formato ISO 2026-09-26")
    void rechazaFormatoIso() {
        assertThrows(DateTimeParseException.class, () -> Fechas.parsear("2026-09-26"));
    }
}
