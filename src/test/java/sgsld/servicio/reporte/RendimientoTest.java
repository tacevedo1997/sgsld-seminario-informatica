package sgsld.servicio.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.dao.FabricaDAO;
import sgsld.dao.memoria.FabricaDAOMemoria;
import sgsld.modelo.Categoria;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Lesion;
import sgsld.modelo.Region;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.Rol;
import sgsld.modelo.TipoLesion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.Usuario;
import sgsld.modelo.ZonaCorporal;
import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.servicio.ReporteService;
import sgsld.util.SesionUsuario;

/**
 * CPS02 · Rendimiento: con un lote sintético de 300 jugadores, 3.000 lesiones
 * con sus tratamientos y 15.000 evoluciones, cada reporte se ejecuta 10 veces
 * y cada ejecución debe tardar menos de 3 segundos.
 */
@Tag("rendimiento")
@DisplayName("CPS02 · Rendimiento de los reportes")
class RendimientoTest {

    private static final int JUGADORES = 300;
    private static final int LESIONES_POR_JUGADOR = 10;
    private static final int EVOLUCIONES_POR_TRATAMIENTO = 5;
    private static final int EJECUCIONES = 10;
    private static final Duration LIMITE = Duration.ofSeconds(3);

    private final FabricaDAO fabrica = new FabricaDAOMemoria();
    private ReporteService servicio;

    @BeforeEach
    void construirLoteSintetico() {
        Random azar = new Random(2026);
        Categoria[] categorias = {new Categoria("Primera", 18, 40), new Categoria("Reserva", 18, 23),
            new Categoria("Sub-17", 16, 17), new Categoria("Sub-15", 14, 15)};
        for (Categoria categoria : categorias) {
            fabrica.getCategoriaDAO().insertar(categoria);
        }
        ZonaCorporal[] zonas = new ZonaCorporal[7];
        for (int i = 0; i < zonas.length; i++) {
            zonas[i] = new ZonaCorporal("Zona " + (i + 1), Region.MIEMBRO_INFERIOR);
            fabrica.getZonaCorporalDAO().insertar(zonas[i]);
        }
        TipoLesion[] tipos = new TipoLesion[5];
        for (int i = 0; i < tipos.length; i++) {
            tipos[i] = new TipoLesion("Tipo " + (i + 1));
            fabrica.getTipoLesionDAO().insertar(tipos[i]);
        }
        Usuario coordinador = new Usuario("coordinador", "sin-hash", Rol.COORDINADOR);
        fabrica.getUsuarioDAO().insertar(coordinador);
        Kinesiologo kinesiologo = new Kinesiologo("Laura", "Fernández", "MP-10234", null);
        fabrica.getKinesiologoDAO().insertar(kinesiologo);

        LocalDate base = LocalDate.of(2025, 1, 1);
        for (int i = 0; i < JUGADORES; i++) {
            Jugador jugador = new Jugador(String.valueOf(30_000_000 + i), "Nombre" + i, "Apellido" + i,
                    LocalDate.of(2000, 1, 1), null, "Defensor", categorias[azar.nextInt(categorias.length)]);
            fabrica.getJugadorDAO().insertar(jugador);
            for (int k = 0; k < LESIONES_POR_JUGADOR; k++) {
                Lesion lesion = new Lesion(jugador, tipos[azar.nextInt(tipos.length)], zonas[azar.nextInt(zonas.length)],
                        Gravedad.LEVE, base.plusDays(azar.nextInt(600)), null);
                fabrica.getLesionDAO().insertar(lesion);
                Tratamiento tratamiento = new Tratamiento(lesion, kinesiologo, lesion.getFecha().plusDays(1),
                        "Fisioterapia", null);
                lesion.agregarTratamiento(tratamiento);
                fabrica.getTratamientoDAO().insertar(tratamiento);
                for (int e = 1; e <= EVOLUCIONES_POR_TRATAMIENTO; e++) {
                    Evolucion evolucion = new Evolucion(tratamiento.getFechaInicio().plusDays(3L * e),
                            "Control " + e, ResultadoEvolucion.ESTABLE);
                    tratamiento.agregarEvolucion(evolucion);
                    fabrica.getEvolucionDAO().insertar(tratamiento.getId(), evolucion);
                }
                if (azar.nextInt(10) < 8) {
                    LocalDate fin = tratamiento.getFechaInicio().plusDays(15 + azar.nextInt(45));
                    tratamiento.finalizar(fin);
                    fabrica.getTratamientoDAO().finalizar(tratamiento.getId(), fin);
                }
            }
        }
        SesionUsuario.getInstancia().iniciar(coordinador);
        servicio = new ReporteService(fabrica.getReporteDAO(), EscenarioPrueba.RELOJ);
    }

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    @Test
    @DisplayName("CPS02 · 10 ejecuciones de cada reporte, todas en menos de 3 segundos")
    void reportesEnMenosDeTresSegundos() throws Exception {
        assertEquals(JUGADORES * LESIONES_POR_JUGADOR, fabrica.getLesionDAO().listarTodos().size());
        int evoluciones = 0;
        for (Tratamiento tratamiento : fabrica.getTratamientoDAO().listarTodos()) {
            evoluciones += tratamiento.getCantidadEvoluciones();
        }
        assertEquals(15_000, evoluciones);

        LocalDate desde = LocalDate.of(2025, 1, 1);
        LocalDate hasta = EscenarioPrueba.HOY;
        for (Reporte reporte : servicio.getReportesDisponibles()) {
            long maximo = 0;
            for (int i = 0; i < EJECUCIONES; i++) {
                long inicio = System.nanoTime();
                List<FilaReporteDTO> filas = assertTimeout(LIMITE, () -> servicio.generar(reporte, desde, hasta),
                        reporte.getTitulo());
                maximo = Math.max(maximo, System.nanoTime() - inicio);
                assertFalse(filas.isEmpty(), reporte.getTitulo());
            }
            System.out.printf("CPS02 · %s: máximo %.1f ms en %d ejecuciones%n",
                    reporte.getTitulo(), maximo / 1_000_000.0, EJECUCIONES);
        }
    }
}
