package sgsld.vista.consola;

import java.io.Console;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;

import sgsld.util.Fechas;
import sgsld.util.Texto;

/**
 * Única puerta de entrada y salida de la aplicación de consola.
 * <p>
 * Lee del teclado con un {@link Scanner} (con el juego de caracteres de la
 * consola, sin forzar UTF-8) y valida cada dato con reintento. Además mantiene
 * la <b>pila de navegación</b> ({@code ArrayDeque} usado como pila) con la que
 * los menús imprimen la ruta, por ejemplo {@code SGSLD > Coordinador > Jugadores}.
 * <p>
 * Con {@code -Dsgsld.eco=true} imprime lo leído después de cada pregunta, para
 * que las transcripciones con la entrada redirigida sean legibles. Al agotarse
 * la entrada, las lecturas lanzan {@link NoSuchElementException}, que la
 * aplicación trata como fin ordenado.
 */
public class Consola {

    /** Ancho de las líneas separadoras. */
    public static final int ANCHO = 72;

    private static final String SEPARADOR_RUTA = " > ";

    private final Scanner scanner;
    private final Console terminal;
    private final boolean eco;
    private final PrintStream salida = System.out;
    private final Deque<String> ruta = new ArrayDeque<>();
    private boolean avisoContrasenaMostrado;

    /**
     * Crea la consola sobre la entrada y la salida estándar.
     *
     * @param eco {@code true} para imprimir lo leído después de cada pregunta
     */
    public Consola(boolean eco) {
        Console consolaSistema = System.console();
        Charset charset = consolaSistema != null ? consolaSistema.charset() : Charset.defaultCharset();
        this.scanner = new Scanner(System.in, charset);
        this.terminal = esTerminalInteractiva(consolaSistema) ? consolaSistema : null;
        this.eco = eco;
    }

    /**
     * Indica si hay una terminal interactiva. Desde el JDK 22,
     * {@code System.console()} devuelve un objeto aun con la entrada redirigida,
     * y lo distingue {@code Console.isTerminal()}, que no existe en Java 17; por
     * eso se consulta por reflexión. En el JDK 17, que exista la consola ya
     * implica una terminal.
     */
    private static boolean esTerminalInteractiva(Console consola) {
        if (consola == null) {
            return false;
        }
        try {
            Method isTerminal = Console.class.getMethod("isTerminal");
            return Boolean.TRUE.equals(isTerminal.invoke(consola));
        } catch (NoSuchMethodException e) {
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ salida

    /**
     * Imprime una línea.
     *
     * @param texto texto a imprimir
     */
    public void mostrar(String texto) {
        salida.println(texto);
    }

    /** Imprime una línea en blanco. */
    public void lineaEnBlanco() {
        salida.println();
    }

    /**
     * Imprime un título de sección subrayado.
     *
     * @param titulo texto del título
     */
    public void titulo(String titulo) {
        salida.println();
        salida.println(titulo);
        salida.println("-".repeat(Math.min(ANCHO, Math.max(titulo.length(), 1))));
    }

    /**
     * Imprime una línea separadora del ancho de la pantalla.
     *
     * @param caracter carácter de la línea
     */
    public void separador(char caracter) {
        salida.println(String.valueOf(caracter).repeat(ANCHO));
    }

    /**
     * Informa una operación exitosa.
     *
     * @param mensaje mensaje a mostrar
     */
    public void mostrarOk(String mensaje) {
        salida.println("[OK] " + mensaje);
    }

    /**
     * Informa un error.
     *
     * @param mensaje mensaje a mostrar
     */
    public void mostrarError(String mensaje) {
        salida.println("[ERROR] " + mensaje);
    }

    /**
     * Informa una lista de errores, uno por línea.
     *
     * @param errores mensajes de error
     */
    public void mostrarErrores(List<String> errores) {
        for (String error : errores) {
            mostrarError(error);
        }
    }

    /**
     * Muestra un aviso que no impide continuar.
     *
     * @param mensaje mensaje a mostrar
     */
    public void mostrarAviso(String mensaje) {
        salida.println("[AVISO] " + mensaje);
    }

    // ------------------------------------------------------- pila de navegación

    /**
     * Apila un nivel de navegación al entrar en un menú.
     *
     * @param nombre nombre del menú
     */
    public void entrarA(String nombre) {
        ruta.push(nombre);
    }

    /** Desapila el último nivel de navegación al salir de un menú. */
    public void salirDeNivel() {
        if (!ruta.isEmpty()) {
            ruta.pop();
        }
    }

    /**
     * Devuelve la ruta de navegación desde la base de la pila.
     *
     * @return por ejemplo, "SGSLD > Coordinador > Jugadores"
     */
    public String getRuta() {
        StringBuilder texto = new StringBuilder();
        Iterator<String> desdeLaBase = ruta.descendingIterator();
        while (desdeLaBase.hasNext()) {
            if (texto.length() > 0) {
                texto.append(SEPARADOR_RUTA);
            }
            texto.append(desdeLaBase.next());
        }
        return texto.toString();
    }

    // ----------------------------------------------------------------- entrada

    /**
     * Lee un número entero. Usa {@code nextInt()}: si se ingresa algo que no
     * es un número, captura {@link InputMismatchException}, informa el error y
     * vuelve a preguntar. El resto de la línea se descarta siempre en el
     * bloque {@code finally}, para que la lectura siguiente empiece limpia.
     *
     * @param pregunta texto de la pregunta
     * @return el número ingresado
     */
    public int leerEntero(String pregunta) {
        while (true) {
            salida.print(pregunta);
            int valor = 0;
            boolean valido = false;
            String leido = "";
            try {
                valor = scanner.nextInt();
                valido = true;
                leido = String.valueOf(valor);
            } catch (InputMismatchException e) {
                // El texto no es un número: queda en el Scanner y lo descarta el finally.
                valido = false;
            } finally {
                String resto = scanner.hasNextLine() ? scanner.nextLine() : "";
                eco(leido + resto);
            }
            if (valido) {
                return valor;
            }
            mostrarError("Ingrese un número válido");
        }
    }

    /**
     * Lee un número entero dentro de un rango, repitiendo hasta obtenerlo.
     *
     * @param pregunta texto de la pregunta
     * @param min      valor mínimo, inclusive
     * @param max      valor máximo, inclusive
     * @return el número ingresado
     */
    public int leerEntero(String pregunta, int min, int max) {
        while (true) {
            int valor = leerEntero(pregunta);
            if (valor >= min && valor <= max) {
                return valor;
            }
            mostrarError("Ingrese un número entre " + min + " y " + max);
        }
    }

    /**
     * Lee un número entero opcional, por línea: Enter devuelve {@code null}.
     * Se usa en los datos que pueden quedar vacíos o conservar su valor.
     *
     * @param pregunta texto de la pregunta
     * @param min      valor mínimo, inclusive
     * @param max      valor máximo, inclusive
     * @return el número, o {@code null} si se presionó Enter
     */
    public Integer leerEnteroOpcional(String pregunta, int min, int max) {
        while (true) {
            String linea = leerLinea(pregunta);
            if (linea.isEmpty()) {
                return null;
            }
            try {
                int valor = Integer.parseInt(linea);
                if (valor >= min && valor <= max) {
                    return valor;
                }
                mostrarError("Ingrese un número entre " + min + " y " + max);
            } catch (NumberFormatException e) {
                mostrarError("Ingrese un número válido");
            }
        }
    }

    /**
     * Lee un texto, sin espacios en los extremos.
     *
     * @param pregunta    texto de la pregunta
     * @param obligatorio si no puede quedar vacío
     * @param maxLongitud cantidad máxima de caracteres
     * @return el texto ingresado; cadena vacía si es opcional y se dejó vacío
     */
    public String leerTexto(String pregunta, boolean obligatorio, int maxLongitud) {
        while (true) {
            String linea = leerLinea(pregunta);
            if (linea.isEmpty() && obligatorio) {
                mostrarError("Este dato es obligatorio");
            } else if (linea.codePointCount(0, linea.length()) > maxLongitud) {
                mostrarError("Admite hasta " + maxLongitud + " caracteres (ingresó "
                        + linea.codePointCount(0, linea.length()) + ")");
            } else {
                return linea;
            }
        }
    }

    /**
     * Lee un dato de texto de un formulario. Si hay un valor actual, lo muestra
     * entre corchetes y Enter lo conserva (modificación o corrección).
     *
     * @param etiqueta    nombre del dato, sin los dos puntos
     * @param valorActual valor actual, o {@code null} en un alta
     * @param obligatorio si no puede quedar vacío
     * @param maxLongitud cantidad máxima de caracteres
     * @return el texto ingresado o el valor actual
     */
    public String editarTexto(String etiqueta, String valorActual, boolean obligatorio, int maxLongitud) {
        if (valorActual == null) {
            return leerTexto(etiqueta + ": ", obligatorio, maxLongitud);
        }
        String linea = leerTexto(etiqueta + " [" + valorActual + "]: ", false, maxLongitud);
        return linea.isEmpty() ? valorActual : linea;
    }

    /**
     * Lee un dato entero de un formulario. Si hay un valor actual, Enter lo
     * conserva; si no, el dato es obligatorio.
     *
     * @param etiqueta    nombre del dato, sin los dos puntos
     * @param valorActual valor actual, o {@code null} en un alta
     * @param min         valor mínimo, inclusive
     * @param max         valor máximo, inclusive
     * @return el número ingresado o el valor actual
     */
    public int editarEntero(String etiqueta, Integer valorActual, int min, int max) {
        if (valorActual == null) {
            return leerEntero(etiqueta + ": ", min, max);
        }
        Integer valor = leerEnteroOpcional(etiqueta + " [" + valorActual + "]: ", min, max);
        return valor == null ? valorActual : valor;
    }

    /**
     * Lee una fecha obligatoria con formato {@code dd/MM/aaaa}. Captura
     * {@link DateTimeParseException} y vuelve a preguntar.
     *
     * @param pregunta texto de la pregunta
     * @return la fecha ingresada
     */
    public LocalDate leerFecha(String pregunta) {
        while (true) {
            String linea = leerLinea(pregunta);
            if (linea.isEmpty()) {
                mostrarError("Este dato es obligatorio");
                continue;
            }
            try {
                return Fechas.parsear(linea);
            } catch (DateTimeParseException e) {
                mostrarError("Fecha inválida. Use el formato dd/mm/aaaa (por ejemplo, 26/09/2026)");
            }
        }
    }

    /**
     * Lee una fecha con un valor por defecto, que se toma al presionar Enter.
     *
     * @param pregunta   texto de la pregunta
     * @param porDefecto fecha que se devuelve si la respuesta queda vacía
     * @return la fecha ingresada o la fecha por defecto
     */
    public LocalDate leerFecha(String pregunta, LocalDate porDefecto) {
        String preguntaConDefecto = pregunta + "[Enter = " + Fechas.formatear(porDefecto) + "]: ";
        while (true) {
            String linea = leerLinea(preguntaConDefecto);
            if (linea.isEmpty()) {
                return porDefecto;
            }
            try {
                return Fechas.parsear(linea);
            } catch (DateTimeParseException e) {
                mostrarError("Fecha inválida. Use el formato dd/mm/aaaa (por ejemplo, 26/09/2026)");
            }
        }
    }

    /**
     * Lee una respuesta S o N.
     *
     * @param pregunta texto de la pregunta (debería terminar en "(S/N): ")
     * @return {@code true} si la respuesta es S
     */
    public boolean leerSiNo(String pregunta) {
        while (true) {
            String respuesta = Texto.normalizar(leerLinea(pregunta));
            if (respuesta.equals("s") || respuesta.equals("si")) {
                return true;
            }
            if (respuesta.equals("n") || respuesta.equals("no")) {
                return false;
            }
            mostrarError("Responda S o N");
        }
    }

    /**
     * Muestra una lista numerada y permite elegir un elemento (0 cancela).
     *
     * @param titulo      texto que precede a la lista
     * @param opciones    elementos a elegir
     * @param descripcion cómo se muestra cada elemento
     * @param <T>         tipo de los elementos
     * @return el elemento elegido, o vacío si se canceló o la lista está vacía
     */
    public <T> Optional<T> elegirDeLista(String titulo, List<T> opciones, Function<T, String> descripcion) {
        if (opciones.isEmpty()) {
            mostrarAviso("No hay elementos para elegir.");
            return Optional.empty();
        }
        mostrar(titulo);
        for (int i = 0; i < opciones.size(); i++) {
            mostrar(String.format("  %2d. %s", i + 1, descripcion.apply(opciones.get(i))));
        }
        mostrar("   0. Cancelar");
        int elegido = leerEntero("Opción: ", 0, opciones.size());
        return elegido == 0 ? Optional.empty() : Optional.of(opciones.get(elegido - 1));
    }

    /**
     * Lee una contraseña. Con terminal interactiva usa
     * {@link Console#readPassword} (no se ve al escribirla); si no, la lee con
     * el {@code Scanner} y avisa que será visible. El llamador debe limpiar el
     * arreglo con {@code Arrays.fill} después de usarlo.
     *
     * @param pregunta texto de la pregunta
     * @return los caracteres de la contraseña
     */
    public char[] leerContrasena(String pregunta) {
        if (terminal != null) {
            char[] contrasena = terminal.readPassword("%s", pregunta);
            if (contrasena == null) {
                throw new NoSuchElementException("Fin de la entrada");
            }
            return contrasena;
        }
        if (!avisoContrasenaMostrado) {
            mostrarAviso("No hay una terminal interactiva: la contraseña será visible al escribirla.");
            avisoContrasenaMostrado = true;
        }
        salida.print(pregunta);
        String linea = scanner.nextLine();
        eco("*".repeat(8));
        return linea.toCharArray();
    }

    /** Espera a que el usuario presione Enter. */
    public void pausa() {
        leerLinea("Presione ENTER para continuar...");
    }

    /** Lee una línea completa, sin espacios en los extremos, y la repite si el eco está activo. */
    private String leerLinea(String pregunta) {
        salida.print(pregunta);
        String linea = scanner.nextLine();
        eco(linea);
        return linea.trim();
    }

    private void eco(String leido) {
        if (eco) {
            salida.println(leido);
        }
    }
}
