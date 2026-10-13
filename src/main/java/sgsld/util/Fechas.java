package sgsld.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Utilidades de fechas: formato {@code dd/MM/aaaa}, zona horaria del club y
 * descripción legible de períodos.
 */
public final class Fechas {

    /** Zona horaria del sistema. */
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");

    /**
     * Formato de entrada y salida {@code dd/MM/aaaa}. Usa {@code uuuu} (año
     * proléptico) y resolución estricta, de modo que fechas inexistentes como
     * 31/02/2026 se rechazan en lugar de ajustarse.
     */
    public static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Fechas() {
    }

    /**
     * Convierte un texto {@code dd/MM/aaaa} en fecha.
     *
     * @param texto texto ingresado
     * @return la fecha correspondiente
     * @throws DateTimeParseException si el formato es inválido o la fecha no existe
     */
    public static LocalDate parsear(String texto) {
        return LocalDate.parse(texto.trim(), FORMATO);
    }

    /**
     * Da formato {@code dd/MM/aaaa} a una fecha.
     *
     * @param fecha fecha a mostrar (puede ser {@code null})
     * @return el texto con formato, o {@code "-"} si la fecha es nula
     */
    public static String formatear(LocalDate fecha) {
        return fecha == null ? "-" : FORMATO.format(fecha);
    }

    /**
     * Describe la distancia entre dos fechas en meses y días, con singular y
     * plural correctos y sin años (14 meses se expresan como "14 meses").
     * Ejemplos: "3 meses y 26 días", "1 mes y 1 día", "1 mes", "26 días",
     * "0 días".
     *
     * @param desde fecha inicial
     * @param hasta fecha final, no anterior a {@code desde}
     * @return la descripción del período
     * @throws IllegalArgumentException si {@code hasta} es anterior a {@code desde}
     */
    public static String describirPeriodo(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        Period periodo = Period.between(desde, hasta);
        long meses = periodo.toTotalMonths();
        int dias = periodo.getDays();
        String textoMeses = meses == 1 ? "1 mes" : meses + " meses";
        String textoDias = dias == 1 ? "1 día" : dias + " días";
        if (meses == 0) {
            return textoDias;
        }
        if (dias == 0) {
            return textoMeses;
        }
        return textoMeses + " y " + textoDias;
    }
}
