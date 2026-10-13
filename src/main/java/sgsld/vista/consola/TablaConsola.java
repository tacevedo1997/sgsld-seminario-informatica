package sgsld.vista.consola;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tabla de texto con bordes ASCII ({@code + - |}) y anchos de columna
 * calculados según el contenido más largo de cada columna.
 */
public class TablaConsola {

    private final List<String> encabezados;
    private final List<List<String>> filas = new ArrayList<>();
    private final Set<Integer> alineadasDerecha = new HashSet<>();
    private List<String> filaTotal;

    /**
     * Crea una tabla con sus encabezados.
     *
     * @param encabezados títulos de las columnas
     */
    public TablaConsola(List<String> encabezados) {
        this.encabezados = List.copyOf(encabezados);
    }

    /**
     * Crea una tabla con sus encabezados.
     *
     * @param encabezados títulos de las columnas
     */
    public TablaConsola(String... encabezados) {
        this(List.of(encabezados));
    }

    /**
     * Alinea a la derecha las columnas indicadas (por ejemplo, las numéricas).
     *
     * @param columnas índices de columna, desde 0
     * @return esta tabla
     */
    public TablaConsola alinearDerecha(int... columnas) {
        for (int columna : columnas) {
            alineadasDerecha.add(columna);
        }
        return this;
    }

    /**
     * Agrega una fila; los valores nulos se muestran como "-".
     *
     * @param celdas valores de la fila, uno por columna
     * @return esta tabla
     */
    public TablaConsola agregarFila(List<String> celdas) {
        filas.add(normalizar(celdas));
        return this;
    }

    /**
     * Agrega una fila; los valores nulos se muestran como "-".
     *
     * @param celdas valores de la fila, uno por columna
     * @return esta tabla
     */
    public TablaConsola agregarFila(String... celdas) {
        return agregarFila(Arrays.asList(celdas));
    }

    /**
     * Define una fila de total, que se imprime separada al final.
     *
     * @param celdas valores de la fila de total
     * @return esta tabla
     */
    public TablaConsola agregarTotal(String... celdas) {
        filaTotal = normalizar(Arrays.asList(celdas));
        return this;
    }

    /**
     * Indica si la tabla no tiene filas de datos.
     *
     * @return {@code true} si está vacía
     */
    public boolean estaVacia() {
        return filas.isEmpty();
    }

    /**
     * Arma las líneas de texto de la tabla.
     *
     * @return las líneas, listas para imprimir
     */
    public List<String> renderizar() {
        int[] anchos = calcularAnchos();
        String borde = borde(anchos);
        List<String> lineas = new ArrayList<>();
        lineas.add(borde);
        lineas.add(linea(encabezados, anchos, false));
        lineas.add(borde);
        for (List<String> fila : filas) {
            lineas.add(linea(fila, anchos, true));
        }
        lineas.add(borde);
        if (filaTotal != null) {
            lineas.add(linea(filaTotal, anchos, true));
            lineas.add(borde);
        }
        return lineas;
    }

    /**
     * Imprime la tabla en la consola.
     *
     * @param consola consola de salida
     */
    public void imprimir(Consola consola) {
        for (String linea : renderizar()) {
            consola.mostrar(linea);
        }
    }

    private List<String> normalizar(List<String> celdas) {
        List<String> fila = new ArrayList<>();
        for (int i = 0; i < encabezados.size(); i++) {
            String celda = i < celdas.size() ? celdas.get(i) : null;
            fila.add(celda == null || celda.isEmpty() ? "-" : celda);
        }
        return fila;
    }

    private int[] calcularAnchos() {
        int[] anchos = new int[encabezados.size()];
        for (int i = 0; i < anchos.length; i++) {
            anchos[i] = encabezados.get(i).length();
            for (List<String> fila : filas) {
                anchos[i] = Math.max(anchos[i], fila.get(i).length());
            }
            if (filaTotal != null) {
                anchos[i] = Math.max(anchos[i], filaTotal.get(i).length());
            }
        }
        return anchos;
    }

    private static String borde(int[] anchos) {
        StringBuilder borde = new StringBuilder("+");
        for (int ancho : anchos) {
            borde.append("-".repeat(ancho + 2)).append('+');
        }
        return borde.toString();
    }

    private String linea(List<String> celdas, int[] anchos, boolean respetarAlineacion) {
        StringBuilder linea = new StringBuilder("|");
        for (int i = 0; i < anchos.length; i++) {
            String celda = celdas.get(i);
            String relleno = " ".repeat(anchos[i] - celda.length());
            boolean derecha = respetarAlineacion && alineadasDerecha.contains(i);
            linea.append(' ').append(derecha ? relleno + celda : celda + relleno).append(" |");
        }
        return linea.toString();
    }
}
