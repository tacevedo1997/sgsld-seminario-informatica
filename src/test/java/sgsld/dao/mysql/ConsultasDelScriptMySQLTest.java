package sgsld.dao.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.Rol;
import sgsld.modelo.Usuario;
import sgsld.modelo.dto.EstadoJugadorDTO;
import sgsld.modelo.dto.FilaReporteDTO;
import sgsld.servicio.Servicios;
import sgsld.servicio.reporte.Reporte;
import sgsld.servicio.reporte.ReporteDuracionPromedio;
import sgsld.servicio.reporte.ReportePorCategoria;
import sgsld.servicio.reporte.ReportePorMes;
import sgsld.servicio.reporte.ReportePorTipo;
import sgsld.servicio.reporte.ReportePorZona;
import sgsld.servicio.reporte.ReporteTratamientosPorEstado;
import sgsld.util.SesionUsuario;

/**
 * Las consultas C3 a C9 de la sección 4 de {@code sgsld_script.sql},
 * ejecutadas tal cual sobre {@code sgsld_test}, dan lo mismo que la aplicación
 * (estado actual y reportes calculados en Java sobre los DAO MySQL). Así el
 * oráculo no depende de valores copiados en la prueba.
 */
@Tag("mysql")
@DisplayName("Consultas C3 a C9 del script = resultados de la aplicación (sgsld_test)")
class ConsultasDelScriptMySQLTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 9, 26);

    private static ConexionBD conexion;
    private static String script;

    @BeforeAll
    static void recrearBase() throws Exception {
        conexion = BaseDePruebasMySQL.recrear();
        script = ScriptBase.leer(ScriptBase.SCRIPT);
    }

    @BeforeEach
    void iniciarSesionComoCoordinador() {
        SesionUsuario.getInstancia().iniciar(new Usuario(1, "mlopez", "-", Rol.COORDINADOR, true));
    }

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    /**
     * Ejecuta una consulta del script, con el período que fija el propio
     * script, y resume cada fila como "columna | columna | ...". Son sentencias
     * fijas del repositorio, sin datos del usuario.
     */
    private static List<String> ejecutarDelScript(String codigo) throws Exception {
        try (Connection conexionSql = conexion.abrir(); Statement sentencia = conexionSql.createStatement()) {
            sentencia.execute(ScriptBase.sentencia(script, "SET @desde"));
            try (ResultSet filas = sentencia.executeQuery(ScriptBase.consulta(script, codigo))) {
                int columnas = filas.getMetaData().getColumnCount();
                List<String> resultado = new ArrayList<>();
                while (filas.next()) {
                    StringBuilder fila = new StringBuilder(filas.getString(1));
                    for (int i = 2; i <= columnas; i++) {
                        fila.append(" | ").append(filas.getString(i));
                    }
                    resultado.add(fila.toString());
                }
                return resultado;
            }
        }
    }

    /** Genera el reporte en Java con el mismo formato que {@link #ejecutarDelScript(String)}. */
    private static List<String> generarEnJava(Class<? extends Reporte> clase) throws Exception {
        Servicios servicios = Servicios.crear(new FabricaDAOMySQL(conexion), EscenarioPrueba.RELOJ);
        for (Reporte reporte : servicios.reportes().getReportesDisponibles()) {
            if (reporte.getClass() == clase) {
                List<String> resultado = new ArrayList<>();
                for (FilaReporteDTO fila : servicios.reportes().generar(reporte, DESDE, HASTA)) {
                    resultado.add(fila.etiqueta() + " | " + fila.cantidad() + (fila.promedio() == null ? ""
                            : " | " + String.format(Locale.ROOT, "%.1f", fila.promedio())));
                }
                return resultado;
            }
        }
        throw new AssertionError("No está disponible " + clase.getSimpleName());
    }

    private static void compararConElScript(String codigo, Class<? extends Reporte> clase) throws Exception {
        List<String> esperado = ejecutarDelScript(codigo);
        assertFalse(esperado.isEmpty(), codigo + " no devolvió filas");
        assertEquals(esperado, generarEnJava(clase), codigo);
    }

    @Test
    @DisplayName("El período del script es el de las pruebas: 01/01/2026 a 26/09/2026")
    void periodoDelScript() {
        assertEquals("SET @desde = '" + DESDE + "', @hasta = '" + HASTA + "'", ScriptBase.sentencia(script, "SET @desde"));
    }

    @Test
    @DisplayName("C3 · v_estado_jugador = consultarEstadoActual (mismas filas, mismo orden)")
    void c3EstadoActual() throws Exception {
        SesionUsuario.getInstancia().iniciar(new Usuario(4, "ctecnico", "-", Rol.CUERPO_TECNICO, true));
        List<String> enJava = new ArrayList<>();
        for (EstadoJugadorDTO fila : Servicios.crear(new FabricaDAOMySQL(conexion), EscenarioPrueba.RELOJ)
                .consultas().consultarEstadoActual()) {
            enJava.add(fila.jugador() + " | " + fila.categoria() + " | " + fila.estado());
        }
        assertEquals(ejecutarDelScript("C3"), enJava);
    }

    @Test
    @DisplayName("C4 · Lesiones por categoría")
    void c4PorCategoria() throws Exception {
        compararConElScript("C4", ReportePorCategoria.class);
    }

    @Test
    @DisplayName("C5 · Lesiones por zona corporal")
    void c5PorZona() throws Exception {
        compararConElScript("C5", ReportePorZona.class);
    }

    @Test
    @DisplayName("C6 · Lesiones por tipo")
    void c6PorTipo() throws Exception {
        compararConElScript("C6", ReportePorTipo.class);
    }

    @Test
    @DisplayName("C7 · Lesiones por mes")
    void c7PorMes() throws Exception {
        compararConElScript("C7", ReportePorMes.class);
    }

    @Test
    @DisplayName("C8 · Duración promedio de tratamientos finalizados por tipo de lesión")
    void c8DuracionPromedio() throws Exception {
        compararConElScript("C8", ReporteDuracionPromedio.class);
    }

    @Test
    @DisplayName("C9 · Tratamientos en curso y finalizados")
    void c9PorEstado() throws Exception {
        compararConElScript("C9", ReporteTratamientosPorEstado.class);
    }
}
