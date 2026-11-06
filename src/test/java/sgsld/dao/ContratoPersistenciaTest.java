package sgsld.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import sgsld.EscenarioPrueba;
import sgsld.modelo.Categoria;
import sgsld.modelo.EstadoDisponibilidad;
import sgsld.modelo.EstadoTratamiento;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Lesion;
import sgsld.modelo.Region;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.Rol;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.Usuario;
import sgsld.modelo.ZonaCorporal;
import sgsld.servicio.AutenticacionService;
import sgsld.servicio.CategoriaService;
import sgsld.servicio.JugadorService;
import sgsld.servicio.KinesiologoService;
import sgsld.servicio.LesionService;
import sgsld.servicio.Servicios;
import sgsld.servicio.TratamientoService;
import sgsld.util.HashContrasena;
import sgsld.util.SesionUsuario;
import sgsld.util.ValidacionException;

/**
 * Contrato de la persistencia: las mismas pruebas de DAO y servicios se
 * ejecutan con los DAO en memoria (siempre) y con MySQL (etiqueta
 * {@code mysql}). Cada subclase indica cómo obtener la fábrica.
 * <p>
 * Las pruebas que modifican datos usan DNI, matrículas y nombres propios, de
 * modo que no se afectan entre sí aunque compartan la base: las lesiones
 * nuevas son de Lucas Molina (sin lesiones en el lote), solo se finaliza el
 * tratamiento 9 y las lecturas clínicas usan a Pedro Gómez y la lesión 7, que
 * ninguna prueba cambia. Los reportes y el estado actual, que dependen de todo
 * el lote, están en {@code ContratoReportesTest}.
 */
public abstract class ContratoPersistenciaTest {

    /**
     * Fábrica con el lote de prueba.
     *
     * @return la fábrica de la prueba actual
     */
    protected abstract FabricaDAO fabrica();

    /**
     * Otra fábrica sobre los mismos datos: en MySQL es una instancia nueva, de
     * modo que releer desde ella demuestra que los cambios quedaron guardados.
     *
     * @return la fábrica para releer
     */
    protected abstract FabricaDAO fabricaNueva();

    @BeforeEach
    void iniciarSesionComoCoordinador() {
        SesionUsuario.getInstancia().iniciar(new Usuario(1, "mlopez", "-", Rol.COORDINADOR, true));
    }

    @AfterEach
    void cerrarSesion() {
        EscenarioPrueba.cerrarSesion();
    }

    private JugadorService jugadores() {
        return new JugadorService(fabrica().getJugadorDAO(), fabrica().getCategoriaDAO(), EscenarioPrueba.RELOJ);
    }

    private Categoria categoria(int id) throws Exception {
        return fabrica().getCategoriaDAO().buscarPorId(id).orElseThrow();
    }

    /** Servicios sobre una sola fábrica, como los arma la aplicación. */
    private Servicios servicios() {
        return Servicios.crear(fabrica(), EscenarioPrueba.RELOJ);
    }

    /** Cambia la sesión a la kinesióloga lfernandez, rol del registro clínico. */
    private static void comoKinesiologo() {
        SesionUsuario.getInstancia().iniciar(new Usuario(2, "lfernandez", "-", Rol.KINESIOLOGO, true));
    }

    private Jugador jugador(int id) throws Exception {
        return fabrica().getJugadorDAO().buscarPorId(id).orElseThrow();
    }

    private Kinesiologo kinesiologo(int id) throws Exception {
        return fabrica().getKinesiologoDAO().buscarPorId(id).orElseThrow();
    }

    @Test
    @DisplayName("Lote · zonas y tipos de lesión en el orden del script")
    void loteCatalogosEnElOrdenDelScript() throws Exception {
        List<String> zonas = new ArrayList<>();
        for (ZonaCorporal zona : fabrica().getZonaCorporalDAO().listarTodos()) {
            zonas.add(zona.getNombre());
        }
        assertEquals(List.of("Isquiotibiales", "Cuádriceps", "Tobillo", "Rodilla", "Aductores", "Hombro",
                "Zona lumbar"), zonas);
        assertEquals(Region.TRONCO, fabrica().getZonaCorporalDAO().buscarPorId(7).orElseThrow().getRegion());
        assertEquals("Ósea", fabrica().getTipoLesionDAO().buscarPorId(4).orElseThrow().getNombre());
        assertEquals(5, fabrica().getTipoLesionDAO().listarTodos().size());
    }

    @Test
    @DisplayName("Lote · jugador por DNI con su categoría; DNI inexistente: vacío")
    void loteJugadorPorDniConSuCategoria() throws Exception {
        Jugador gomez = fabrica().getJugadorDAO().buscarPorDni("42587634").orElseThrow();
        assertEquals("Gómez, Pedro", gomez.getNombreCompleto());
        assertEquals(LocalDate.of(2000, 7, 22), gomez.getFechaNacimiento());
        assertEquals(5, gomez.getNumeroCamiseta());
        assertEquals("Mediocampista", gomez.getPosicion());
        assertTrue(gomez.isActivo());
        assertEquals("Primera", gomez.getCategoria().getNombre());
        assertEquals("18 a 40 años", gomez.getCategoria().getRangoEtario());
        assertTrue(fabrica().getJugadorDAO().buscarPorDni("12345678").isEmpty());
    }

    @Test
    @DisplayName("Lote · búsqueda por apellido parcial, sin tildes ni mayúsculas")
    void loteBusquedaPorApellidoSinTildesNiMayusculas() throws Exception {
        List<String> apellidos = new ArrayList<>();
        for (Jugador jugador : fabrica().getJugadorDAO().buscarPorApellido("GOMEZ")) {
            apellidos.add(jugador.getApellido());
        }
        assertEquals(List.of("Gómez"), apellidos);
        List<String> conRo = new ArrayList<>();
        for (Jugador jugador : fabrica().getJugadorDAO().buscarPorApellido("ro")) {
            conRo.add(jugador.getApellido());
        }
        assertTrue(conRo.containsAll(List.of("Romero", "Castro")));
    }

    @Test
    @DisplayName("Lote · kinesiólogos con usuario (Fernández) y sin usuario (Ruiz)")
    void loteKinesiologosConYSinUsuario() throws Exception {
        Kinesiologo fernandez = fabrica().getKinesiologoDAO().buscarPorMatricula("mp-10234").orElseThrow();
        assertEquals("Fernández, Laura", fernandez.getNombreCompleto());
        assertEquals("lfernandez", fernandez.getUsuario().getNombreUsuario());
        assertEquals(Rol.KINESIOLOGO, fernandez.getUsuario().getRol());
        assertFalse(fabrica().getKinesiologoDAO().buscarPorMatricula("MP-12950").orElseThrow().tieneUsuario());
        assertEquals(fernandez, fabrica().getKinesiologoDAO().buscarPorUsuario(2).orElseThrow());
    }

    @Test
    @DisplayName("Lote · usuario ctecnico y categoría por nombre sin distinguir mayúsculas")
    void loteUsuariosYCategorias() throws Exception {
        Usuario usuario = fabrica().getUsuarioDAO().buscarPorNombreUsuario("ctecnico").orElseThrow();
        assertEquals(Rol.CUERPO_TECNICO, usuario.getRol());
        assertTrue(usuario.isActivo());
        Categoria sub17 = fabrica().getCategoriaDAO().buscarPorNombre("sub-17").orElseThrow();
        assertEquals("Sub-17", sub17.getNombre());
        assertEquals(16, sub17.getEdadMin());
    }

    @Test
    @DisplayName("CPI01 · lfernandez inicia sesión con el hash PBKDF2 del lote")
    void autenticacionConElHashDelLote() throws Exception {
        Usuario usuario = new AutenticacionService(fabrica().getUsuarioDAO())
                .iniciarSesion("lfernandez", EscenarioPrueba.CONTRASENA_LOTE.toCharArray());
        assertEquals(Rol.KINESIOLOGO, usuario.getRol());
    }

    @Test
    @DisplayName("Alta de jugador persistida: se relee desde otra fábrica")
    void altaDeJugadorPersistida() throws Exception {
        Jugador nuevo = new Jugador("40100100", "Martín", "Contrato", LocalDate.of(2001, 5, 10), 14, "Defensor",
                categoria(1));
        int id = jugadores().registrar(nuevo);
        Jugador releido = fabricaNueva().getJugadorDAO().buscarPorDni("40100100").orElseThrow();
        assertEquals(id, releido.getId());
        assertEquals("Contrato, Martín", releido.getNombreCompleto());
        assertEquals(LocalDate.of(2001, 5, 10), releido.getFechaNacimiento());
        assertEquals(14, releido.getNumeroCamiseta());
        assertEquals("Primera", releido.getCategoria().getNombre());
    }

    @Test
    @DisplayName("Modificación y baja lógica persistidas")
    void modificacionYBajaLogicaPersistidas() throws Exception {
        int id = jugadores().registrar(new Jugador("40100200", "Luis", "Modificado", LocalDate.of(2002, 1, 2), null,
                "Delantero", categoria(1)));
        jugadores().modificar(new Jugador(id, "40100200", "Luis", "Modificado", LocalDate.of(2002, 1, 2), 21,
                "Arquero", true, categoria(2)));
        Jugador releido = fabricaNueva().getJugadorDAO().buscarPorId(id).orElseThrow();
        assertEquals(21, releido.getNumeroCamiseta());
        assertEquals("Arquero", releido.getPosicion());
        assertEquals("Reserva", releido.getCategoria().getNombre());

        jugadores().darDeBaja(id);
        assertFalse(fabricaNueva().getJugadorDAO().buscarPorId(id).orElseThrow().isActivo());
        for (Jugador activo : fabricaNueva().getJugadorDAO().listarActivos()) {
            assertTrue(activo.getId() != id);
        }
    }

    @Test
    @DisplayName("CPC06 · DNI 42587634 duplicado: ValidacionException y el original no cambia")
    void dniDuplicadoRechazado() throws Exception {
        Jugador duplicado = new Jugador("42587634", "Otro", "Jugador", LocalDate.of(2001, 5, 10), null, "Defensor",
                categoria(1));
        ValidacionException error = assertThrows(ValidacionException.class, () -> jugadores().registrar(duplicado));
        assertEquals(List.of("Ya existe un jugador con DNI 42587634."), error.getErrores());
        assertEquals("Gómez", fabricaNueva().getJugadorDAO().buscarPorDni("42587634").orElseThrow().getApellido());
    }

    @Test
    @DisplayName("CPC06 · Categoría PRIMERA duplicada: ValidacionException")
    void categoriaDuplicadaSinDistinguirMayusculas() {
        CategoriaService categorias = new CategoriaService(fabrica().getCategoriaDAO());
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> categorias.registrar(new Categoria("PRIMERA", 18, 40)));
        assertEquals(List.of("Ya existe una categoría con el nombre Primera."), error.getErrores());
    }

    @Test
    @DisplayName("Alta de kinesiólogo con su usuario (transacción) persistida")
    void altaDeKinesiologoConUsuarioPersistida() throws Exception {
        KinesiologoService kinesiologos = new KinesiologoService(fabrica().getKinesiologoDAO(),
                fabrica().getUsuarioDAO());
        kinesiologos.registrar(new Kinesiologo("Ana", "Ríos", "MP-13001", null), "arios", "clave1234".toCharArray());
        Kinesiologo releido = fabricaNueva().getKinesiologoDAO().buscarPorMatricula("MP-13001").orElseThrow();
        assertEquals("Ríos, Ana", releido.getNombreCompleto());
        assertEquals("arios", releido.getUsuario().getNombreUsuario());
        Usuario usuario = fabricaNueva().getUsuarioDAO().buscarPorNombreUsuario("arios").orElseThrow();
        assertEquals(Rol.KINESIOLOGO, usuario.getRol());
        assertTrue(HashContrasena.verificar("clave1234", usuario.getHashContrasena()));
    }

    @Test
    @DisplayName("CPC06 · Matrícula MP-10234 duplicada: ValidacionException")
    void matriculaDuplicadaRechazada() {
        KinesiologoService kinesiologos = new KinesiologoService(fabrica().getKinesiologoDAO(),
                fabrica().getUsuarioDAO());
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> kinesiologos.registrar(new Kinesiologo("Otra", "Persona", "MP-10234", null)));
        assertEquals(List.of("Ya existe un kinesiólogo con matrícula MP-10234."), error.getErrores());
    }

    @Test
    @DisplayName("C1 · Historial de Pedro Gómez: lesiones 2 y 7 con tratamientos, kinesiólogo y evoluciones")
    void historialConElGrafoCompleto() throws Exception {
        comoKinesiologo();
        List<Lesion> historial = servicios().lesiones().consultarHistorial(2);
        assertEquals(2, historial.size());

        Lesion lesion2 = historial.get(0);
        assertEquals(2, lesion2.getId());
        assertEquals(LocalDate.of(2026, 1, 10), lesion2.getFecha());
        assertEquals("Gómez, Pedro", lesion2.getJugador().getNombreCompleto());
        assertEquals("Primera", lesion2.getJugador().getCategoria().getNombre());
        assertEquals("Muscular", lesion2.getTipo().getNombre());
        assertEquals("Isquiotibiales", lesion2.getZona().getNombre());
        assertEquals(Gravedad.MODERADA, lesion2.getGravedad());
        assertEquals("Distensión en isquiotibiales durante un sprint", lesion2.getDescripcion());
        Tratamiento tratamiento2 = lesion2.getUltimoTratamiento();
        assertEquals(EstadoTratamiento.FINALIZADO, tratamiento2.getEstado());
        assertEquals(LocalDate.of(2026, 2, 20), tratamiento2.getFechaFin());
        assertEquals(40, tratamiento2.getDuracionDias());
        assertEquals("Fernández, Laura", tratamiento2.getKinesiologo().getNombreCompleto());
        assertEquals(3, tratamiento2.getCantidadEvoluciones());
        assertEquals("Completa pruebas de campo; alta", tratamiento2.getEvoluciones().get(2).getObservacion());

        Lesion lesion7 = historial.get(1);
        assertEquals(7, lesion7.getId());
        assertEquals(Gravedad.GRAVE, lesion7.getGravedad());
        assertEquals(1, lesion7.getTratamientos().size());
        Tratamiento tratamiento7 = lesion7.getUltimoTratamiento();
        assertTrue(tratamiento7.estaEnCurso());
        assertNull(tratamiento7.getFechaFin());
        assertEquals("Fisioterapia y fortalecimiento excéntrico", tratamiento7.getTipo());
        List<ResultadoEvolucion> resultados = new ArrayList<>();
        for (Evolucion evolucion : tratamiento7.getEvoluciones()) {
            resultados.add(evolucion.getResultado());
        }
        assertEquals(List.of(ResultadoEvolucion.ESTABLE, ResultadoEvolucion.DESFAVORABLE,
                ResultadoEvolucion.FAVORABLE, ResultadoEvolucion.FAVORABLE), resultados);
    }

    @Test
    @DisplayName("C2 · Recurrencia con los antecedentes leídos: Gómez (caso A) alerta la lesión 2; Acosta (caso B), no")
    void recurrenciaConLosAntecedentesLeidos() throws Exception {
        comoKinesiologo();
        LesionService lesiones = servicios().lesiones();
        Lesion casoA = new Lesion(jugador(2), lesiones.listarTipos().get(0), lesiones.listarZonas().get(0),
                Gravedad.LEVE, LocalDate.of(2026, 6, 15), null);
        assertEquals(2, lesiones.verificarRecurrencia(casoA, lesiones.buscarAntecedentes(casoA)).orElseThrow().getId());
        Lesion casoB = new Lesion(jugador(6), lesiones.listarTipos().get(0), lesiones.listarZonas().get(0),
                Gravedad.LEVE, LocalDate.of(2026, 7, 1), null);
        assertTrue(lesiones.verificarRecurrencia(casoB, lesiones.buscarAntecedentes(casoB)).isEmpty());
    }

    @Test
    @DisplayName("CU-04 a CU-06 · Lesión, tratamiento, evolución y fin de Lucas Molina persistidos")
    void registroClinicoPersistido() throws Exception {
        comoKinesiologo();
        Servicios servicios = servicios();
        Lesion lesion = new Lesion(jugador(8), servicios.lesiones().listarTipos().get(0),
                servicios.lesiones().listarZonas().get(1), Gravedad.LEVE, LocalDate.of(2026, 9, 20),
                "Sobrecarga en cuádriceps (contrato)");
        int idLesion = servicios.lesiones().registrarLesion(lesion);
        int idTratamiento = servicios.tratamientos().registrarTratamiento(new Tratamiento(lesion, kinesiologo(1),
                LocalDate.of(2026, 9, 21), "Readaptación funcional", null));
        servicios.tratamientos().registrarEvolucion(idTratamiento,
                new Evolucion(LocalDate.of(2026, 9, 24), "Sin dolor en carrera continua", ResultadoEvolucion.FAVORABLE));

        List<Lesion> releidas = fabricaNueva().getLesionDAO().buscarPorJugador(8);
        assertEquals(1, releidas.size());
        Lesion releida = releidas.get(0);
        assertEquals(idLesion, releida.getId());
        assertEquals("Cuádriceps", releida.getZona().getNombre());
        assertEquals("Sobrecarga en cuádriceps (contrato)", releida.getDescripcion());
        Tratamiento tratamiento = releida.getUltimoTratamiento();
        assertEquals(idTratamiento, tratamiento.getId());
        assertTrue(tratamiento.estaEnCurso());
        assertEquals("Sin dolor en carrera continua", tratamiento.getEvoluciones().get(0).getObservacion());
        assertTrue(fabricaNueva().getTratamientoDAO().existeActivo(idLesion));
        assertEquals(EstadoDisponibilidad.EN_TRATAMIENTO, fabricaNueva().getEstadoJugadorDAO().estadoDe(8));

        assertEquals(4, servicios.tratamientos().finalizarTratamiento(idTratamiento, LocalDate.of(2026, 9, 25))
                .getDuracionDias());
        Tratamiento finalizado = fabricaNueva().getTratamientoDAO().buscarPorId(idTratamiento).orElseThrow();
        assertEquals(EstadoTratamiento.FINALIZADO, finalizado.getEstado());
        assertEquals(LocalDate.of(2026, 9, 25), finalizado.getFechaFin());
        assertEquals(4, finalizado.getDuracionDias());
        assertFalse(fabricaNueva().getTratamientoDAO().existeActivo(idLesion));
        assertEquals(EstadoDisponibilidad.DISPONIBLE, fabricaNueva().getEstadoJugadorDAO().estadoDe(8));
    }

    @Test
    @DisplayName("CPC04-3 · Lesión 7 con un tratamiento en curso: el segundo se rechaza y no se guarda")
    void segundoTratamientoEnCursoRechazado() throws Exception {
        comoKinesiologo();
        Lesion lesion7 = fabrica().getLesionDAO().buscarPorId(7).orElseThrow();
        Tratamiento segundo = new Tratamiento(lesion7, kinesiologo(1), LocalDate.of(2026, 9, 20),
                "Readaptación funcional", null);
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> servicios().tratamientos().registrarTratamiento(segundo));
        assertEquals(List.of("La lesión N° 7 ya tiene un tratamiento en curso. "
                + "Debe finalizarlo antes de registrar uno nuevo."), error.getErrores());
        assertEquals(1, fabricaNueva().getLesionDAO().buscarPorId(7).orElseThrow().getTratamientos().size());
    }

    @Test
    @DisplayName("Registrar y finalizar · tratamiento 9: la evolución y el fin quedan guardados juntos")
    void registrarYFinalizarPersistido() throws Exception {
        comoKinesiologo();
        Evolucion ultima = new Evolucion(LocalDate.of(2026, 9, 20), "Estabilidad articular completa; alta",
                ResultadoEvolucion.FAVORABLE);
        int idEvolucion = servicios().tratamientos().registrarEvolucionYFinalizar(9, ultima, LocalDate.of(2026, 9, 20));
        assertTrue(idEvolucion > 11);
        assertEquals(idEvolucion, ultima.getId());

        Tratamiento tratamiento9 = fabricaNueva().getTratamientoDAO().buscarPorId(9).orElseThrow();
        assertEquals(EstadoTratamiento.FINALIZADO, tratamiento9.getEstado());
        assertEquals(LocalDate.of(2026, 9, 20), tratamiento9.getFechaFin());
        assertEquals(48, tratamiento9.getDuracionDias());
        assertEquals(3, tratamiento9.getCantidadEvoluciones());
        Evolucion releida = tratamiento9.getEvoluciones().get(2);
        assertEquals(idEvolucion, releida.getId());
        assertEquals("Estabilidad articular completa; alta", releida.getObservacion());
        assertEquals(EstadoDisponibilidad.DISPONIBLE, fabricaNueva().getEstadoJugadorDAO().estadoDe(5));
    }

    @Test
    @DisplayName("Registrar y finalizar · fin anterior a la evolución: rechazo y no se guarda nada")
    void registrarYFinalizarRechazado() throws Exception {
        comoKinesiologo();
        TratamientoService tratamientos = servicios().tratamientos();
        Evolucion evolucion = new Evolucion(LocalDate.of(2026, 9, 20), "No debe quedar guardada",
                ResultadoEvolucion.ESTABLE);
        ValidacionException error = assertThrows(ValidacionException.class,
                () -> tratamientos.registrarEvolucionYFinalizar(7, evolucion, LocalDate.of(2026, 9, 19)));
        assertEquals(List.of("La fecha de fin no puede ser anterior a la fecha de la evolución (20/09/2026)."),
                error.getErrores());
        Tratamiento tratamiento7 = fabricaNueva().getTratamientoDAO().buscarPorId(7).orElseThrow();
        assertTrue(tratamiento7.estaEnCurso());
        assertEquals(4, tratamiento7.getCantidadEvoluciones());
        assertEquals(0, evolucion.getId());
    }
}