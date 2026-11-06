package sgsld.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Exporta una matriz de textos a un archivo CSV que Excel en español abre
 * bien: codificación UTF-8 con BOM, separador punto y coma y fin de línea
 * CRLF. Los valores con separador, comillas o saltos de línea van entre
 * comillas, con las comillas internas duplicadas (RFC 4180).
 */
public final class ExportadorCSV {

    /** Separador de campos (Excel en español usa la coma como separador decimal). */
    public static final char SEPARADOR = ';';
    /** Marca de orden de bytes: le indica a Excel que el archivo es UTF-8. */
    private static final char BOM = '﻿';
    private static final String FIN_DE_LINEA = "\r\n";

    private ExportadorCSV() {
    }

    /**
     * Escribe la matriz en el archivo (lo crea o lo reemplaza).
     *
     * @param matriz  filas × columnas (la primera fila, los encabezados)
     * @param archivo archivo de destino
     * @throws IOException si no se puede escribir
     */
    public static void exportar(String[][] matriz, Path archivo) throws IOException {
        try (BufferedWriter salida = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
            salida.write(BOM);
            for (String[] fila : matriz) {
                for (int columna = 0; columna < fila.length; columna++) {
                    if (columna > 0) {
                        salida.write(SEPARADOR);
                    }
                    salida.write(escapar(fila[columna]));
                }
                salida.write(FIN_DE_LINEA);
            }
        }
    }

    /**
     * Prepara un valor para el CSV.
     *
     * @param valor texto de la celda (puede ser {@code null})
     * @return el valor, entre comillas si hace falta
     */
    static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        boolean entreComillas = valor.indexOf(SEPARADOR) >= 0 || valor.indexOf('"') >= 0
                || valor.indexOf('\n') >= 0 || valor.indexOf('\r') >= 0;
        return entreComillas ? '"' + valor.replace("\"", "\"\"") + '"' : valor;
    }

    /**
     * Nombre sugerido para el archivo: {@code reporte-<criterio>-<desde>-<hasta>.csv},
     * con el criterio sin tildes ni espacios y las fechas en formato aaaa-MM-dd.
     *
     * @param criterio título del reporte
     * @param desde    fecha inicial (o la de la consulta, si el reporte no usa período)
     * @param hasta    fecha final, o {@code null} si el reporte no usa período
     * @return el nombre del archivo
     */
    public static String nombreSugerido(String criterio, LocalDate desde, LocalDate hasta) {
        String base = Texto.normalizar(criterio).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return "reporte-" + base + "-" + desde + (hasta == null ? "" : "-" + hasta) + ".csv";
    }
}
