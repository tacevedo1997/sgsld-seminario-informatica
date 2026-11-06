package sgsld;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import sgsld.controlador.CategoriaController;
import sgsld.controlador.Controladores;
import sgsld.controlador.EstadoActualController;
import sgsld.controlador.FichaJugadorController;
import sgsld.controlador.JugadorController;
import sgsld.controlador.KinesiologoController;
import sgsld.controlador.LoginController;
import sgsld.controlador.RegistroLesionController;
import sgsld.controlador.ReporteController;
import sgsld.controlador.SelectorJugador;
import sgsld.controlador.TratamientoController;
import sgsld.dao.FabricaDAO;
import sgsld.dao.memoria.DatosDePrueba;
import sgsld.dao.memoria.FabricaDAOMemoria;
import sgsld.dao.mysql.ConexionBD;
import sgsld.dao.mysql.FabricaDAOMySQL;
import sgsld.servicio.Servicios;
import sgsld.util.Fechas;
import sgsld.util.PersistenciaException;
import sgsld.util.RegistroTecnico;
import sgsld.vista.MenuPrincipal;
import sgsld.vista.consola.Consola;

/**
 * Raíz de composición del SGSLD y punto de entrada de la vista de consola.
 * <p>
 * Es la única clase que elige la implementación concreta de la persistencia:
 * {@link FabricaDAOMySQL} (por defecto) o {@link FabricaDAOMemoria} con
 * {@code -Dsgsld.datos=memoria}. Si MySQL no responde, lo informa: no cae en
 * silencio a la memoria. Arma los servicios y, para la consola, los
 * controladores y menús; la interfaz JavaFX usa los mismos métodos.
 * <p>
 * Propiedades del sistema: {@code -Dsgsld.datos=mysql|memoria},
 * {@code -Dsgsld.hoy=aaaa-MM-dd} (fecha fija, evidencias reproducibles) y
 * {@code -Dsgsld.eco=true} (repite la entrada leída en la consola).
 */
public final class App {

    /** Propiedad que elige el origen de los datos. */
    public static final String PROPIEDAD_DATOS = "sgsld.datos";

    /** Origen de los datos de la aplicación. */
    public enum ModoDatos {
        /** Base MySQL configurada en {@code db.properties}. */
        MYSQL,
        /** Lote de prueba en memoria, para demostrar la aplicación sin servidor. */
        MEMORIA
    }

    private App() {
    }

    /**
     * Inicia la vista de consola. Si la entrada se agota, termina
     * ordenadamente; ante un error inesperado informa el problema y sale con
     * código 1.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        try {
            ejecutarConsola();
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Fin de la entrada: la aplicación se cerró.");
        } catch (RuntimeException e) {
            RegistroTecnico.logger().log(Level.SEVERE, "Error inesperado", e);
            System.err.println("[ERROR] Error inesperado: " + e);
            System.exit(1);
        }
    }

    private static void ejecutarConsola() {
        List<String> avisos = new ArrayList<>();
        Clock reloj = crearReloj(avisos);
        ModoDatos modo = modoDatos(avisos);
        configurarRegistro(avisos);
        avisos.add(0, describirDatos(modo));

        Consola consola = new Consola(Boolean.getBoolean("sgsld.eco"));
        MenuPrincipal.mostrarPantallaInicial(consola, avisos);
        FabricaDAO fabrica = conectar(consola, modo);
        if (fabrica == null) {
            consola.mostrar("La aplicación se cerró sin conexión a la base de datos.");
            return;
        }
        Servicios servicios = Servicios.crear(fabrica, reloj);
        MenuPrincipal menu = new MenuPrincipal(consola, crearControladores(consola, servicios));
        menu.mostrar();
        consola.mostrar("Hasta luego.");
    }

    /** Crea la fábrica; si falla la conexión, informa el motivo y ofrece reintentar. */
    private static FabricaDAO conectar(Consola consola, ModoDatos modo) {
        while (true) {
            try {
                return crearFabrica(modo);
            } catch (PersistenciaException e) {
                consola.mostrarError(e.getMessage());
                if (!consola.leerSiNo("¿Reintentar la conexión? (S/N): ")) {
                    return null;
                }
            }
        }
    }

    private static Controladores crearControladores(Consola consola, Servicios servicios) {
        SelectorJugador selector = new SelectorJugador(consola, servicios.jugadores());
        TratamientoController tratamientos = new TratamientoController(consola, servicios.tratamientos(),
                servicios.lesiones(), servicios.kinesiologos(), servicios.consultas(), selector);
        return new Controladores(
                new LoginController(consola, servicios.autenticacion()),
                new JugadorController(consola, servicios.jugadores(), servicios.categorias(), selector),
                new CategoriaController(consola, servicios.categorias()),
                new KinesiologoController(consola, servicios.kinesiologos()),
                new RegistroLesionController(consola, servicios.lesiones(), selector, tratamientos),
                tratamientos,
                new FichaJugadorController(consola, servicios.consultas(), selector),
                new EstadoActualController(consola, servicios.consultas()),
                new ReporteController(consola, servicios.reportes()));
    }

    /**
     * Crea la fábrica de DAO del modo indicado. En MySQL lee
     * {@code db.properties} y prueba la conexión antes de devolverla.
     *
     * @param modo origen de los datos
     * @return la fábrica
     * @throws PersistenciaException si falta la configuración o MySQL no responde
     */
    public static FabricaDAO crearFabrica(ModoDatos modo) throws PersistenciaException {
        if (modo == ModoDatos.MEMORIA) {
            FabricaDAOMemoria fabrica = new FabricaDAOMemoria();
            DatosDePrueba.cargar(fabrica);
            return fabrica;
        }
        ConexionBD conexion = ConexionBD.getInstancia();
        conexion.probarConexion();
        return new FabricaDAOMySQL(conexion);
    }

    /**
     * Lee {@code -Dsgsld.datos}: {@code mysql} (o sin valor) o {@code memoria}.
     * Un valor desconocido se informa y se usa MySQL.
     *
     * @param avisos lista donde se agrega el aviso, si corresponde
     * @return el modo de datos
     */
    public static ModoDatos modoDatos(List<String> avisos) {
        String valor = System.getProperty(PROPIEDAD_DATOS, "mysql").trim().toLowerCase(Locale.ROOT);
        if (valor.equals("memoria")) {
            return ModoDatos.MEMORIA;
        }
        if (!valor.equals("mysql")) {
            avisos.add("[AVISO] El valor de sgsld.datos (" + valor + ") no es mysql ni memoria; se usa MySQL.");
        }
        return ModoDatos.MYSQL;
    }

    /**
     * Texto de la pantalla inicial que indica de dónde vienen los datos.
     *
     * @param modo origen de los datos
     * @return la descripción
     */
    public static String describirDatos(ModoDatos modo) {
        return modo == ModoDatos.MEMORIA
                ? "Modo demostración: datos de prueba en memoria; los cambios no se conservan al salir."
                : "Datos: base MySQL configurada en db.properties; los cambios se guardan.";
    }

    /**
     * Crea el reloj del sistema, o uno fijo si se indicó {@code -Dsgsld.hoy}.
     * Un valor inválido se informa y se usa el reloj del sistema.
     *
     * @param avisos lista donde se agrega el aviso de la fecha fijada
     * @return el reloj
     */
    public static Clock crearReloj(List<String> avisos) {
        String valor = System.getProperty("sgsld.hoy");
        if (valor == null || valor.isBlank()) {
            return Clock.system(Fechas.ZONA);
        }
        try {
            LocalDate fecha = LocalDate.parse(valor.trim());
            avisos.add("Fecha del sistema fijada en " + Fechas.formatear(fecha) + ".");
            return Clock.fixed(fecha.atStartOfDay(Fechas.ZONA).toInstant(), Fechas.ZONA);
        } catch (DateTimeParseException e) {
            avisos.add("[AVISO] El valor de sgsld.hoy (" + valor + ") no es una fecha aaaa-MM-dd válida; "
                    + "se usa el reloj del sistema.");
            return Clock.system(Fechas.ZONA);
        }
    }

    /**
     * Configura el log técnico en {@code logs/sgsld.log}.
     *
     * @param avisos lista donde se agrega un aviso si no se pudo crear el archivo
     */
    public static void configurarRegistro(List<String> avisos) {
        if (!RegistroTecnico.configurar(Path.of("logs"))) {
            avisos.add("[AVISO] No se pudo crear logs/sgsld.log: los errores técnicos no quedarán registrados.");
        }
    }
}
