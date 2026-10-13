package sgsld.vista.consola;

import java.util.ArrayList;
import java.util.List;

/**
 * Gráfico de barras horizontales en ASCII: cada barra se dibuja con {@code #},
 * escalada al ancho máximo según el valor mayor, con el valor al final.
 */
public class GraficoConsola {

    /** Ancho máximo de barra por defecto, en caracteres. */
    public static final int ANCHO_MAXIMO = 40;

    private final int anchoMaximo;
    private final List<String> etiquetas = new ArrayList<>();
    private final List<Double> valores = new ArrayList<>();
    private final List<String> textosValor = new ArrayList<>();

    /** Crea un gráfico con el ancho máximo por defecto. */
    public GraficoConsola() {
        this(ANCHO_MAXIMO);
    }

    /**
     * Crea un gráfico con un ancho máximo de barra.
     *
     * @param anchoMaximo cantidad de {@code #} de la barra más larga
     */
    public GraficoConsola(int anchoMaximo) {
        this.anchoMaximo = anchoMaximo;
    }

    /**
     * Agrega una barra.
     *
     * @param etiqueta   texto a la izquierda de la barra
     * @param valor      valor que determina el largo
     * @param textoValor valor tal como se muestra al final de la barra
     * @return este gráfico
     */
    public GraficoConsola agregar(String etiqueta, double valor, String textoValor) {
        etiquetas.add(etiqueta);
        valores.add(valor);
        textosValor.add(textoValor);
        return this;
    }

    /**
     * Arma las líneas del gráfico.
     *
     * @return las líneas, listas para imprimir
     */
    public List<String> renderizar() {
        double maximo = 0;
        int anchoEtiqueta = 0;
        for (int i = 0; i < valores.size(); i++) {
            maximo = Math.max(maximo, valores.get(i));
            anchoEtiqueta = Math.max(anchoEtiqueta, etiquetas.get(i).length());
        }
        List<String> lineas = new ArrayList<>();
        for (int i = 0; i < valores.size(); i++) {
            double valor = valores.get(i);
            int largo = maximo == 0 ? 0 : (int) Math.round(valor / maximo * anchoMaximo);
            if (valor > 0 && largo == 0) {
                largo = 1;
            }
            String etiqueta = etiquetas.get(i);
            lineas.add(etiqueta + " ".repeat(anchoEtiqueta - etiqueta.length()) + " | "
                    + "#".repeat(largo) + " " + textosValor.get(i));
        }
        return lineas;
    }

    /**
     * Imprime el gráfico en la consola.
     *
     * @param consola consola de salida
     */
    public void imprimir(Consola consola) {
        for (String linea : renderizar()) {
            consola.mostrar(linea);
        }
    }
}
