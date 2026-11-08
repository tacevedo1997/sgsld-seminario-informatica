package sgsld.vista.fx;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;

import sgsld.App;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Jugador;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.TipoLesion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.ZonaCorporal;
import sgsld.servicio.reporte.Reporte;
import sgsld.servicio.reporte.ReporteDuracionPromedio;
import sgsld.servicio.reporte.ReporteTratamientosPorEstado;
import sgsld.util.SgsldException;
import sgsld.util.Texto;

/**
 * Herramienta de desarrollo, fuera del flujo normal de la aplicación: abre
 * cada pantalla con el lote de prueba en memoria y la fecha fija del plan de
 * pruebas (26/09/2026), guarda una imagen con {@code snapshot()} y termina.
 * Sirve para comparar las pantallas con los prototipos del informe.
 * <p>
 * Además recorre algunos escenarios como lo haría un usuario (completa campos
 * y presiona botones) y verifica el resultado: alta con errores y alta
 * correcta, baja lógica, nombre duplicado y contraseñas distintas. Los
 * diálogos no esperan al usuario: se guardan como imagen y se responde con el
 * botón por defecto.
 * <p>
 * Todo ocurre en el hilo de JavaFX: las tareas de las pantallas se ejecutan en
 * el mismo hilo y, antes de cada imagen, se procesan los eventos pendientes
 * hasta que llegan todos sus resultados ({@link #esperarTareas()}).
 * <p>
 * Uso: {@code java -cp target/sgsld.jar sgsld.vista.fx.Capturador [carpeta]}
 * (por defecto, {@code target/capturas}). Con {@code -Dsgsld.datos=mysql} y
 * {@code -Dsgsld.config=db-test.properties} recorre los mismos escenarios
 * sobre una base {@code sgsld_test} recién creada; con otra base se niega a
 * ejecutar, porque los escenarios registran datos.
 */
public final class Capturador {

    /** Contraseña de los usuarios del lote de prueba (sgsld_script.sql). */
    private static final String CONTRASENA_LOTE = "Sgsld2026!";
    private static final String[] USUARIOS = {"mlopez", "lfernandez", "ctecnico"};
    private static final double ESCALA = 2;
    /** Única base MySQL sobre la que puede trabajar: los escenarios registran datos. */
    private static final String BASE_DE_PRUEBAS = "sgsld_test";
    /** Mensaje de ejemplo para la captura del diálogo de error de conexión. */
    private static final String MENSAJE_SIN_SERVIDOR = "No se pudo conectar con el servidor MySQL. "
            + "Verifique que el servicio esté en ejecución y que la dirección de db.properties sea correcta.";

    private final Path destino;
    private final List<Path> guardadas = new ArrayList<>();
    private final List<String> dialogos = new ArrayList<>();
    private ContextoFX contexto;
    private Stage ventana;
    private int numero = 1;
    /** Diálogos que se esperan, en orden: nombre de la imagen y botón a elegir ({@code null}: el por defecto). */
    private final Deque<DialogoEsperado> esperados = new ArrayDeque<>();

    private record DialogoEsperado(String nombre, ButtonType respuesta) {
    }

    private Capturador(Path destino) {
        this.destino = destino;
    }

    /**
     * Genera las capturas y recorre los escenarios.
     *
     * @param args carpeta de destino (opcional)
     * @throws Exception si falla alguna captura o un escenario no da el resultado esperado
     */
    public static void main(String[] args) throws Exception {
        if (System.getProperty(App.PROPIEDAD_DATOS) == null) {
            System.setProperty(App.PROPIEDAD_DATOS, "memoria");
        }
        if (System.getProperty("sgsld.hoy") == null) {
            System.setProperty("sgsld.hoy", "2026-09-26");
        }
        Path destino = args.length > 0 ? Path.of(args[0]) : Path.of("target", "capturas");
        Files.createDirectories(destino);
        // Solo las capturas anteriores de esta herramienta (nombres "NN-...png").
        try (var anteriores = Files.newDirectoryStream(destino, "[0-9][0-9]-*.png")) {
            for (Path anterior : anteriores) {
                Files.delete(anterior);
            }
        }
        Locale.setDefault(Texto.LOCALE);
        Lanzador.silenciarAvisoDeConfiguracion();
        Capturador capturador = new Capturador(destino);
        CompletableFuture<List<Path>> resultado = new CompletableFuture<>();
        Platform.startup(() -> {
            try {
                resultado.complete(capturador.capturar());
            } catch (Exception | Error e) {
                resultado.completeExceptionally(e);
            }
        });
        try {
            for (Path archivo : resultado.get()) {
                System.out.println("Captura: " + archivo);
            }
        } finally {
            Platform.exit();
        }
    }

    private List<Path> capturar() throws IOException, SgsldException {
        List<String> avisos = new ArrayList<>();
        Clock reloj = App.crearReloj(avisos);
        App.ModoDatos modo = App.modoDatos(avisos);
        if (modo == App.ModoDatos.MYSQL && !BASE_DE_PRUEBAS.equals(App.baseConfigurada())) {
            throw new IllegalStateException("Por seguridad, con MySQL el Capturador solo usa la base "
                    + BASE_DE_PRUEBAS + " (registra datos de prueba); la configuración apunta a '"
                    + App.baseConfigurada() + "'. Use -Dsgsld.config=db-test.properties.");
        }
        ventana = new Stage();
        contexto = new ContextoFX(ventana, reloj, modo, avisos, Runnable::run);
        contexto.usarPresentador(this::presentar);
        // Exportar CSV guarda en la carpeta de las capturas, con el nombre sugerido.
        contexto.usarSelectorDeArchivos(selector -> destino.resolve(selector.getInitialFileName()).toFile());
        contexto.conectar();

        contexto.mostrarLogin();
        ventana.show();
        esperarTareas();
        guardar("login");
        recorrerPantallas();
        guardar(DialogoErrorConexion.crear(ventana, MENSAJE_SIN_SERVIDOR), "error-conexion");

        iniciarSesion("mlopez");
        escenarioJugadores();
        escenarioCategorias();
        escenarioKinesiologos();
        escenarioReportes();
        escenarioFicha();
        cerrarSesion();

        iniciarSesion("lfernandez");
        escenarioRegistroLesion();
        escenarioEvoluciones();
        cerrarSesion();
        ventana.close();
        return guardadas;
    }

    /** Cada usuario del lote abre todas sus opciones; en administración, también una fila elegida. */
    private void recorrerPantallas() throws SgsldException {
        for (String usuario : USUARIOS) {
            PrincipalController principal = iniciarSesion(usuario);
            for (OpcionMenu opcion : principal.opcionesHabilitadas()) {
                ControladorPantalla pantalla = principal.abrir(opcion);
                esperarTareas();
                String nombre = usuario + "-" + opcion.archivoFxml().replace(".fxml", "").replace('_', '-');
                guardar(nombre);
                if (pantalla instanceof ControladorAdministracion<?> administracion) {
                    administracion.seleccionarFila(0);
                    guardar(nombre + "-modificar");
                }
            }
            cerrarSesion();
        }
    }

    // ---------- Escenarios ----------

    /** CU-01: alta con errores (todos juntos), alta correcta y baja lógica con confirmación. */
    private void escenarioJugadores() throws SgsldException {
        abrir("Jugadores");
        presionar("botonNuevo");
        escribir("campoDni", "42587634");
        escribir("campoApellido", "Suárez");
        escribir("campoNombre", "Martín");
        escribirFecha("campoNacimiento", "10/05/2001");
        guardarConDialogo("botonGuardar", "jugadores-alta-con-errores");
        exigirDialogo("Ya existe un jugador con DNI 42587634.", "El campo posición es obligatorio.",
                "El campo categoría es obligatorio.");

        escribir("campoDni", "43111222");
        escribir("campoCamiseta", "6");
        escribir("campoPosicion", "Defensor");
        elegir("comboCategoria", 0);
        presionar("botonGuardar");
        esperarTareas();
        guardar("jugadores-alta-correcta");
        exigir(tablaContiene("43111222"), "el jugador nuevo no aparece en la tabla");
        exigir(textoDe("etiquetaResultado").startsWith("Jugador registrado con el N° 9"), "falta el mensaje de alta");

        guardarConDialogo("botonBaja", "jugadores-confirmar-baja");
        exigirDialogo("¿Dar de baja a Suárez, Martín (DNI 43111222)?");
        guardar("jugadores-baja");
        exigir(!tablaContiene("43111222"), "el jugador dado de baja sigue en la tabla");
        exigir(textoDe("tituloFormulario").equals("NUEVO JUGADOR"), "el formulario sigue mostrando al dado de baja");
        marcar("checkIncluirBaja");
        esperarTareas();
        exigir(tablaContiene("Dado de baja"), "\"Incluir dados de baja\" no muestra al jugador");
        guardar("jugadores-incluir-dados-de-baja");
    }

    /** CU-02: nombre repetido (sin distinguir mayúsculas) y alta correcta. */
    private void escenarioCategorias() throws SgsldException {
        abrir("Categorías");
        presionar("botonNuevo");
        escribir("campoNombre", "PRIMERA");
        escribir("campoEdadMinima", "18");
        escribir("campoEdadMaxima", "40");
        guardarConDialogo("botonGuardar", "categorias-nombre-repetido");
        exigirDialogo("Ya existe una categoría con el nombre Primera.");
        escribir("campoNombre", "Sub-19");
        escribir("campoEdadMaxima", "19");
        presionar("botonGuardar");
        esperarTareas();
        exigir(tablaContiene("Sub-19"), "la categoría nueva no aparece en la tabla");
        guardar("categorias-alta-correcta");
    }

    /** CU-03: contraseñas distintas y alta con usuario del sistema (en una transacción). */
    private void escenarioKinesiologos() throws SgsldException {
        abrir("Kinesiólogos");
        presionar("botonNuevo");
        escribir("campoApellido", "Ríos");
        escribir("campoNombre", "Ana");
        escribir("campoMatricula", "MP-13001");
        marcar("checkCrearUsuario");
        escribir("campoUsuario", "arios");
        escribir("campoContrasena", "clave1234");
        escribir("campoRepeticion", "clave4321");
        guardar("kinesiologos-alta-con-usuario");
        guardarConDialogo("botonGuardar", "kinesiologos-contrasenas-distintas");
        exigirDialogo("Las contraseñas no coinciden.");
        escribir("campoRepeticion", "clave1234");
        presionar("botonGuardar");
        esperarTareas();
        exigir(tablaContiene("arios"), "el kinesiólogo nuevo no aparece con su usuario");
        guardar("kinesiologos-alta-correcta");
    }

    /**
     * CU-07 (Figura 13): reporte por zona con su gráfico y Total, exportación a
     * CSV, promedio con decimales, reporte sin período, período sin datos y
     * período invertido.
     */
    private void escenarioReportes() throws IOException {
        abrir("Reportes");
        exigir(tablaContiene("tabla", "Sub-17"), "no se generó el primer reporte al abrir la pantalla");
        elegirEn("comboCriterio", reporte -> reporte instanceof Reporte r && r.getTitulo().equals("Lesiones por zona corporal"));
        presionar("botonGenerar");
        esperarTareas();
        exigir(textoDe("tituloGrafico").equals("Lesiones por zona corporal · 01/01/2026 – 26/09/2026"),
                "título del gráfico: " + textoDe("tituloGrafico"));
        exigir(tablaContiene("tabla", "Isquiotibiales") && tablaContiene("tabla", "Total"), "falta la tabla por zona");
        guardar("mlopez-reportes-figura-13");

        presionarConDialogos("botonExportar", new DialogoEsperado("mlopez-reportes-csv-exportado", null));
        Path csv = destino.resolve("reporte-lesiones-por-zona-corporal-2026-01-01-2026-09-26.csv");
        String contenido = Files.readString(csv, StandardCharsets.UTF_8);
        String esperado = (char) 0xFEFF + "Zona corporal;Lesiones\r\nIsquiotibiales;3\r\n";
        exigir(contenido.startsWith(esperado) && contenido.endsWith("Total;8\r\n"),
                "el CSV no tiene el formato esperado:\n" + contenido);
        guardadas.add(csv);

        elegirEn("comboCriterio", reporte -> reporte instanceof ReporteDuracionPromedio);
        presionar("botonGenerar");
        esperarTareas();
        exigir(tablaContiene("tabla", "31,5"), "falta el promedio con coma decimal");
        guardar("mlopez-reportes-duracion-promedio");

        elegirEn("comboCriterio", reporte -> reporte instanceof ReporteTratamientosPorEstado);
        exigir(buscar("campoDesde", DatePicker.class).isDisabled(), "el reporte sin período no deshabilita las fechas");
        presionar("botonGenerar");
        esperarTareas();
        exigir(textoDe("tituloGrafico").endsWith("· situación actual"), "falta \"situación actual\" en el título");
        guardar("mlopez-reportes-tratamientos-por-estado");

        elegirEn("comboCriterio", reporte -> reporte instanceof Reporte r && r.getTitulo().equals("Lesiones por zona corporal"));
        escribirFecha("campoDesde", "01/01/2024");
        escribirFecha("campoHasta", "31/12/2024");
        presionar("botonGenerar");
        esperarTareas();
        exigir(textoDe("etiquetaSinResultados").equals("No se encontraron resultados para los filtros seleccionados."),
                "falta el mensaje de período sin datos");
        guardar("mlopez-reportes-sin-resultados");

        escribirFecha("campoDesde", "26/09/2026");
        escribirFecha("campoHasta", "01/01/2026");
        guardarConDialogo("botonGenerar", "mlopez-reportes-periodo-invertido");
        exigirDialogo("La fecha desde no puede ser posterior a la fecha hasta.");
    }

    /** CU-10: búsqueda por apellido, datos con el chip de estado, historial, tratamientos y evoluciones. */
    private void escenarioFicha() {
        abrir("Consultar ficha");
        escribir("campoBusqueda", "gomez");
        presionar("botonBuscar");
        esperarTareas();
        exigir(textoDe("etiquetaNombre").equals("Gómez, Pedro"), "la búsqueda por apellido no mostró la ficha");
        exigir(tablaContiene("tablaHistorial", "Distensión en isquiotibiales durante un sprint"), "falta el historial");
        exigir(textoDe("tituloTratamientos").contains("LESIÓN N° 7"), "no quedó elegida la lesión más reciente");
        exigir(buscar("tablaEvoluciones", TableView.class).getItems().size() == 4, "faltan las evoluciones");
        guardar("mlopez-ficha-gomez");
        guardarPagina("mlopez-ficha-gomez-completa");
    }

    /**
     * CU-04, CU-11 y CU-05: el caso de la Figura 11 (Pedro Gómez, isquiotibiales,
     * 15/06/2026) muestra la alerta por la lesión del 10/01/2026; luego se
     * registra el tratamiento desde "¿Registrar el tratamiento ahora?".
     */
    private void escenarioRegistroLesion() throws SgsldException {
        abrir("Registrar lesión");
        elegirEn("comboJugador", jugador -> jugador instanceof Jugador j && j.getDni().equals("42587634"));
        esperarTareas();
        exigir(tablaContiene("tablaHistorial", "Fisioterapia y readaptación"), "no se cargó el historial de Gómez");
        elegirEn("comboTipo", tipo -> tipo instanceof TipoLesion t && t.getNombre().equals("Muscular"));
        elegirEn("comboZona", zona -> zona instanceof ZonaCorporal z && z.getNombre().equals("Isquiotibiales"));
        elegirEn("comboGravedad", gravedad -> gravedad == Gravedad.GRAVE);
        escribirFecha("campoFecha", "15/06/2026");
        escribir("campoDescripcion", "Dolor agudo en sprint durante el partido. Dificultad para la marcha.");
        guardar("lfernandez-registrar-lesion-figura-11");
        presionarConDialogos("botonRegistrar", new DialogoEsperado("lfernandez-alerta-recurrencia", null),
                new DialogoEsperado("lfernandez-registrar-tratamiento-ahora", null));
        exigir(dialogos.get(dialogos.size() - 2).contains("Isquiotibiales · 10/01/2026, tratamiento finalizado el "
                + "20/02/2026\n(3 meses y 26 días antes de la nueva lesión)."), "la alerta no coincide con la Figura 11:\n"
                + dialogos.get(dialogos.size() - 2));
        exigirDialogo("Lesión N° 11 registrada", "¿Registrar el tratamiento ahora?");

        exigir(textoDe("tituloDatos").contains("LESIÓN N° 11"), "Tratamientos no recibió la lesión recién registrada");
        guardar("lfernandez-tratamientos-desde-la-lesion");
        escribir("campoTipo", "Fisioterapia y fortalecimiento excéntrico");
        presionar("botonRegistrar");
        esperarTareas();
        exigir(textoDe("etiquetaResultado").equals("Tratamiento N° 11 registrado (En curso)."),
                "falta el mensaje del tratamiento registrado");
        exigir(tablaContiene("tablaLesiones", "Tratamiento en curso"), "la lesión no quedó con tratamiento en curso");
        guardar("lfernandez-tratamientos-alta-correcta");
        escribir("campoTipo", "Readaptación funcional");
        presionarConDialogos("botonRegistrar", new DialogoEsperado("lfernandez-tratamientos-segundo-en-curso", null));
        exigirDialogo("La lesión N° 11 ya tiene un tratamiento en curso.");
    }

    /**
     * CU-06: evolución simple (el tratamiento sigue en curso), "Registrar y
     * finalizar" cancelado (no se guarda nada) y confirmado (una transacción).
     */
    private void escenarioEvoluciones() {
        abrir("Evoluciones");
        elegirFila("tablaTratamientos", fila -> fila instanceof Tratamiento t && t.getId() == 7);
        escribirFecha("campoFecha", "25/09/2026");
        elegirEn("comboResultado", resultado -> resultado == ResultadoEvolucion.FAVORABLE);
        escribir("campoObservacion", "Completa trabajo con pelota");
        presionar("botonRegistrar");
        esperarTareas();
        exigir(textoDe("etiquetaResultado").endsWith("El tratamiento N° 7 sigue En curso."), "falta el mensaje");
        exigir(buscar("tablaEvoluciones", TableView.class).getItems().size() == 5, "no aparece la evolución nueva");
        guardar("lfernandez-evoluciones-registrada");

        elegirFila("tablaTratamientos", fila -> fila instanceof Tratamiento t && t.getId() == 9);
        escribirFecha("campoFecha", "20/09/2026");
        elegirEn("comboResultado", resultado -> resultado == ResultadoEvolucion.FAVORABLE);
        escribir("campoObservacion", "Estabilidad articular completa; alta deportiva");
        presionarConDialogos("botonFinalizar",
                new DialogoEsperado("lfernandez-finalizar-cancelado", DialogoFinalizacion.CANCELAR));
        exigir(textoDe("etiquetaResultado").startsWith("No se guardó nada"), "falta el aviso de cancelación");
        exigir(buscar("tablaEvoluciones", TableView.class).getItems().size() == 2, "se guardó la evolución cancelada");
        exigir(tablaContiene("tablaTratamientos", "Herrera, Tomás"), "el tratamiento cancelado dejó de estar en curso");

        guardar("lfernandez-evoluciones-registrar-y-finalizar");
        presionarConDialogos("botonFinalizar", new DialogoEsperado("lfernandez-confirmar-finalizacion", null),
                new DialogoEsperado("lfernandez-tratamiento-finalizado", null));
        exigirDialogo("Duración: 48 días", "Estado actual de Herrera, Tomás: Disponible.");
        exigir(!tablaContiene("tablaTratamientos", "Herrera, Tomás"), "el tratamiento finalizado sigue en curso");
        guardar("lfernandez-evoluciones-despues-de-finalizar");
    }

    // ---------- Acciones de usuario ----------

    private PrincipalController iniciarSesion(String usuario) throws SgsldException {
        contexto.getServicios().autenticacion().iniciarSesion(usuario, CONTRASENA_LOTE.toCharArray());
        contexto.mostrarPrincipal();
        esperarTareas();
        return principal();
    }

    private void cerrarSesion() {
        contexto.getServicios().autenticacion().cerrarSesion();
    }

    private PrincipalController principal() {
        return (PrincipalController) contexto.getPantallaActual().controlador();
    }

    private void abrir(String textoMenu) {
        for (OpcionMenu opcion : principal().opcionesHabilitadas()) {
            if (opcion.texto().equals(textoMenu)) {
                principal().abrir(opcion);
                esperarTareas();
                return;
            }
        }
        throw new IllegalStateException("El menú no tiene la opción " + textoMenu);
    }

    /** Busca un control por su fx:id; antes aplica CSS para que existan las pieles (el ScrollPane, por ejemplo). */
    private <T extends Node> T buscar(String id, Class<T> tipo) {
        ventana.getScene().getRoot().applyCss();
        ventana.getScene().getRoot().layout();
        Node nodo = ventana.getScene().lookup("#" + id);
        if (!tipo.isInstance(nodo)) {
            throw new IllegalStateException("No se encontró #" + id + " (" + tipo.getSimpleName() + ")");
        }
        return tipo.cast(nodo);
    }

    private void presionar(String id) {
        buscar(id, ButtonBase.class).fire();
    }

    private void marcar(String id) {
        CheckBox casilla = buscar(id, CheckBox.class);
        if (!casilla.isSelected()) {
            casilla.fire();
        }
    }

    private void escribir(String id, String texto) {
        buscar(id, TextInputControl.class).setText(texto);
    }

    private void escribirFecha(String id, String texto) {
        buscar(id, DatePicker.class).getEditor().setText(texto);
    }

    private void elegir(String id, int indice) {
        buscar(id, ComboBox.class).getSelectionModel().select(indice);
    }

    private String textoDe(String id) {
        return buscar(id, Label.class).getText();
    }

    /** Elige en el combo el primer elemento que cumple la condición. */
    private void elegirEn(String id, Predicate<Object> condicion) {
        ComboBox<?> combo = buscar(id, ComboBox.class);
        for (int i = 0; i < combo.getItems().size(); i++) {
            if (condicion.test(combo.getItems().get(i))) {
                combo.getSelectionModel().select(i);
                return;
            }
        }
        throw new IllegalStateException("#" + id + " no tiene el elemento buscado");
    }

    /** Selecciona en la tabla la primera fila que cumple la condición. */
    private void elegirFila(String id, Predicate<Object> condicion) {
        TableView<?> tabla = buscar(id, TableView.class);
        for (int i = 0; i < tabla.getItems().size(); i++) {
            if (condicion.test(tabla.getItems().get(i))) {
                tabla.getSelectionModel().select(i);
                return;
            }
        }
        throw new IllegalStateException("#" + id + " no tiene la fila buscada");
    }

    /** Presiona el botón, procesa las tareas y guarda la imagen del diálogo que aparezca. */
    private void guardarConDialogo(String idBoton, String nombre) {
        presionarConDialogos(idBoton, new DialogoEsperado(nombre, null));
    }

    /**
     * Presiona el botón y procesa las tareas; verifica que aparezcan los
     * diálogos esperados, en orden, y los responde.
     */
    private void presionarConDialogos(String idBoton, DialogoEsperado... dialogosEsperados) {
        int vistos = dialogos.size();
        esperados.addAll(List.of(dialogosEsperados));
        presionar(idBoton);
        esperarTareas();
        exigir(esperados.isEmpty(), "no apareció el diálogo esperado (" + (esperados.isEmpty() ? ""
                : esperados.peekFirst().nombre()) + ")");
        exigir(dialogos.size() == vistos + dialogosEsperados.length, "apareció un diálogo de más:\n"
                + dialogos.get(dialogos.size() - 1));
    }

    private void exigirDialogo(String... fragmentos) {
        String ultimo = dialogos.get(dialogos.size() - 1);
        for (String fragmento : fragmentos) {
            exigir(ultimo.contains(fragmento), "el diálogo no dice \"" + fragmento + "\":\n" + ultimo);
        }
    }

    private boolean tablaContiene(String texto) {
        return tablaContiene("tabla", texto);
    }

    private boolean tablaContiene(String id, String texto) {
        TableView<?> tabla = buscar(id, TableView.class);
        for (int fila = 0; fila < tabla.getItems().size(); fila++) {
            for (var columna : tabla.getColumns()) {
                Object valor = columna.getCellData(fila);
                if (valor != null && valor.toString().contains(texto)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void exigir(boolean condicion, String problema) {
        if (!condicion) {
            throw new IllegalStateException("Escenario fallido: " + problema);
        }
    }

    /**
     * Muestra el diálogo sin esperar, guarda su imagen, registra su texto y
     * responde con el botón por defecto (Aceptar, Sí, Reintentar...).
     */
    private Optional<ButtonType> presentar(Dialog<ButtonType> dialogo) {
        DialogoEsperado esperado = esperados.pollFirst();
        dialogo.show();
        dialogos.add(textoVisible(dialogo.getDialogPane()));
        guardar(dialogo, esperado == null ? "dialogo-inesperado" : esperado.nombre());
        ButtonType respuesta = dialogo.getDialogPane().getButtonTypes().get(0);
        for (ButtonType boton : dialogo.getDialogPane().getButtonTypes()) {
            if (boton.getButtonData().isDefaultButton()) {
                respuesta = boton;
                break;
            }
        }
        if (esperado != null && esperado.respuesta() != null) {
            respuesta = esperado.respuesta();
        }
        dialogo.setResult(respuesta);
        dialogo.close();
        return Optional.of(respuesta);
    }

    /** Todo el texto visible de un nodo (etiquetas y textos), en orden. */
    private static String textoVisible(Node nodo) {
        StringBuilder texto = new StringBuilder();
        if (nodo instanceof Labeled etiqueta && etiqueta.getText() != null && !etiqueta.getText().isEmpty()) {
            texto.append(etiqueta.getText()).append('\n');
        } else if (nodo instanceof Text parte) {
            texto.append(parte.getText());
        }
        if (nodo instanceof Parent padre) {
            for (Node hijo : padre.getChildrenUnmodifiable()) {
                texto.append(textoVisible(hijo));
            }
        }
        return texto.toString();
    }

    /**
     * Procesa los eventos pendientes del hilo de JavaFX (entre ellos, los
     * resultados de las tareas) hasta que no quede ninguna tarea en curso.
     */
    private void esperarTareas() {
        do {
            Object clave = new Object();
            Platform.runLater(() -> Platform.exitNestedEventLoop(clave, null));
            Platform.enterNestedEventLoop(clave);
        } while (contexto.hayTareasEnCurso());
    }

    // ---------- Imágenes ----------

    /** Guarda la pantalla completa aunque no entre en la ventana (el contenido del área desplazable). */
    private void guardarPagina(String nombre) {
        Node contenido = buscarPorClase("contenido-desplazable", ScrollPane.class).getContent();
        guardarNodo(contenido, nombre, Color.web("#FAFBFB"));
    }

    private <T extends Node> T buscarPorClase(String clase, Class<T> tipo) {
        Node nodo = ventana.getScene().lookup("." + clase);
        if (!tipo.isInstance(nodo)) {
            throw new IllegalStateException("No se encontró ." + clase);
        }
        return tipo.cast(nodo);
    }

    private void guardar(String nombre) {
        Scene escena = ventana.getScene();
        guardarNodo(escena.getRoot(), nombre);
    }

    private void guardar(Dialog<ButtonType> dialogo, String nombre) {
        if (dialogo instanceof Alert alerta && !alerta.isShowing()) {
            alerta.show();
            guardarNodo(alerta.getDialogPane(), nombre);
            alerta.close();
        } else {
            guardarNodo(dialogo.getDialogPane(), nombre);
        }
    }

    private void guardarNodo(Node nodo, String nombre) {
        guardarNodo(nodo, nombre, Color.WHITE);
    }

    /** Guarda la imagen del nodo en PNG, al doble de resolución, con un número correlativo. */
    private void guardarNodo(Node nodo, String nombre, Color fondo) {
        Path archivo = destino.resolve(String.format("%02d-%s.png", numero++, nombre));
        SnapshotParameters parametros = new SnapshotParameters();
        parametros.setTransform(Transform.scale(ESCALA, ESCALA));
        parametros.setFill(fondo);
        WritableImage imagen = nodo.snapshot(parametros, null);
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        int[] pixeles = new int[ancho * alto];
        imagen.getPixelReader().getPixels(0, 0, ancho, alto, PixelFormat.getIntArgbInstance(), pixeles, 0, ancho);
        BufferedImage salida = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        salida.setRGB(0, 0, ancho, alto, pixeles, 0, ancho);
        try {
            ImageIO.write(salida, "png", archivo.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + archivo, e);
        }
        guardadas.add(archivo);
    }
}
