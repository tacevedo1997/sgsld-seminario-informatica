package sgsld;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
import sgsld.dao.memoria.DatosDePrueba;
import sgsld.dao.memoria.FabricaDAOMemoria;
import sgsld.servicio.AutenticacionService;
import sgsld.servicio.CategoriaService;
import sgsld.servicio.ConsultaService;
import sgsld.servicio.JugadorService;
import sgsld.servicio.KinesiologoService;
import sgsld.servicio.LesionService;
import sgsld.servicio.ReporteService;
import sgsld.servicio.TratamientoService;
import sgsld.util.Fechas;
import sgsld.vista.MenuPrincipal;
import sgsld.vista.consola.Consola;

/**
 * Raíz de composición y punto de entrada del SGSLD.
 * <p>
 * Es la única clase que conoce la implementación concreta de la persistencia
 * (a través de {@link FabricaDAOMemoria}): crea la fábrica de DAO, carga el
 * lote de prueba y arma servicios, controladores y menús.
 * <p>
 * Propiedades del sistema: {@code -Dsgsld.eco=true} repite la entrada leída
 * (transcripciones con entrada redirigida) y {@code -Dsgsld.hoy=aaaa-MM-dd}
 * fija la fecha del sistema (evidencias reproducibles).
 */
public final class App {

    private App() {
    }

    /**
     * Inicia la aplicación. Si la entrada se agota, termina ordenadamente; ante
     * un error inesperado informa el problema y sale con código 1.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        try {
            ejecutar();
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Fin de la entrada: la aplicación se cerró.");
        } catch (RuntimeException e) {
            System.err.println("[ERROR] Error inesperado: " + e);
            System.exit(1);
        }
    }

    private static void ejecutar() {
        List<String> avisos = new ArrayList<>();
        Clock reloj = crearReloj(avisos);

        FabricaDAOMemoria fabrica = new FabricaDAOMemoria();
        DatosDePrueba.cargar(fabrica);

        AutenticacionService autenticacion = new AutenticacionService(fabrica.getUsuarioDAO());
        JugadorService jugadorService = new JugadorService(fabrica.getJugadorDAO(), fabrica.getCategoriaDAO(), reloj);
        CategoriaService categoriaService = new CategoriaService(fabrica.getCategoriaDAO());
        KinesiologoService kinesiologoService = new KinesiologoService(fabrica.getKinesiologoDAO(),
                fabrica.getUsuarioDAO());
        LesionService lesionService = new LesionService(fabrica.getLesionDAO(), fabrica.getJugadorDAO(),
                fabrica.getZonaCorporalDAO(), fabrica.getTipoLesionDAO(), reloj);
        TratamientoService tratamientoService = new TratamientoService(fabrica.getTratamientoDAO(),
                fabrica.getLesionDAO(), fabrica.getKinesiologoDAO(), fabrica.getEvolucionDAO(), reloj);
        ConsultaService consultaService = new ConsultaService(fabrica.getEstadoJugadorDAO(), fabrica.getJugadorDAO(),
                lesionService, reloj);
        ReporteService reporteService = new ReporteService(fabrica.getReporteDAO(), reloj);

        Consola consola = new Consola(Boolean.getBoolean("sgsld.eco"));
        SelectorJugador selector = new SelectorJugador(consola, jugadorService);
        TratamientoController tratamientoController = new TratamientoController(consola, tratamientoService,
                lesionService, kinesiologoService, consultaService, selector);
        Controladores controladores = new Controladores(
                new LoginController(consola, autenticacion),
                new JugadorController(consola, jugadorService, categoriaService, selector),
                new CategoriaController(consola, categoriaService),
                new KinesiologoController(consola, kinesiologoService),
                new RegistroLesionController(consola, lesionService, selector, tratamientoController),
                tratamientoController,
                new FichaJugadorController(consola, consultaService, selector),
                new EstadoActualController(consola, consultaService),
                new ReporteController(consola, reporteService));

        MenuPrincipal menu = new MenuPrincipal(consola, controladores);
        menu.mostrarPantallaInicial(avisos);
        menu.mostrar();
        consola.mostrar("Hasta luego.");
    }

    /**
     * Crea el reloj del sistema, o uno fijo si se indicó {@code -Dsgsld.hoy}.
     * Un valor inválido se informa y se usa el reloj del sistema.
     */
    private static Clock crearReloj(List<String> avisos) {
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
}
