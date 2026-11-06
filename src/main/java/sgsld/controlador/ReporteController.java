package sgsld.controlador;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.servicio.ReporteService;
import sgsld.servicio.reporte.Reporte;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.Fechas;
import sgsld.util.PersistenciaException;
import sgsld.util.ValidacionException;
import sgsld.vista.consola.Consola;
import sgsld.vista.consola.GraficoConsola;
import sgsld.vista.consola.TablaConsola;

/**
 * Reportes estadísticos (CU-07). Trabaja con la lista polimórfica de
 * {@link Reporte}: no conoce las clases concretas; cada reporte define sus
 * encabezados, celdas, total y valor graficado.
 */
public class ReporteController {

    private final Consola consola;
    private final ReporteService reportes;

    /**
     * Crea el controlador.
     *
     * @param consola  entrada y salida
     * @param reportes servicio de reportes
     */
    public ReporteController(Consola consola, ReporteService reportes) {
        this.consola = consola;
        this.reportes = reportes;
    }

    /**
     * Pide el período (Enter en "desde" = 01/01 del año actual; Enter en
     * "hasta" = hoy) y permite generar reportes uno tras otro, con la opción de
     * cambiar el período entre uno y otro.
     *
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public void generarReportes() throws AccesoDenegadoException, PersistenciaException {
        consola.titulo("Reportes estadísticos");
        List<Reporte> disponibles = reportes.getReportesDisponibles();
        LocalDate hoy = reportes.hoy();
        LocalDate desde = consola.leerFecha("Desde ", hoy.withDayOfYear(1));
        LocalDate hasta = consola.leerFecha("Hasta ", hoy);
        while (true) {
            Optional<Reporte> elegido = consola.elegirDeLista("Reportes disponibles:", disponibles, Reporte::getTitulo);
            if (elegido.isEmpty()) {
                return;
            }
            boolean periodoValido = true;
            try {
                mostrar(elegido.get(), reportes.generar(elegido.get(), desde, hasta), desde, hasta);
            } catch (ValidacionException e) {
                consola.mostrarErrores(e.getErrores());
                periodoValido = false;
            }
            if (periodoValido && !consola.leerSiNo("¿Generar otro reporte? (S/N): ")) {
                return;
            }
            if (!periodoValido || consola.leerSiNo("¿Cambiar el período? (S/N): ")) {
                desde = consola.leerFecha("Desde ", hoy.withDayOfYear(1));
                hasta = consola.leerFecha("Hasta ", hoy);
            }
        }
    }

    private void mostrar(Reporte reporte, List<FilaReporteDTO> filas, LocalDate desde, LocalDate hasta) {
        consola.titulo(reporte.getTitulo() + (reporte.usaPeriodo()
                ? " (" + Fechas.formatear(desde) + " a " + Fechas.formatear(hasta) + ")"
                : " (situación actual)"));
        if (filas.isEmpty()) {
            consola.mostrarAviso("No se encontraron resultados para los filtros seleccionados.");
            return;
        }
        TablaConsola tabla = new TablaConsola(reporte.getEncabezados()).alinearDerecha(1, 2);
        GraficoConsola grafico = new GraficoConsola();
        long total = 0;
        for (FilaReporteDTO fila : filas) {
            List<String> celdas = reporte.getCeldas(fila);
            tabla.agregarFila(celdas);
            grafico.agregar(fila.etiqueta(), reporte.getValorGrafico(fila), celdas.get(celdas.size() - 1));
            total += fila.cantidad();
        }
        if (reporte.muestraTotal()) {
            tabla.agregarTotal("Total", String.valueOf(total));
        }
        tabla.imprimir(consola);
        consola.lineaEnBlanco();
        grafico.imprimir(consola);
    }
}
