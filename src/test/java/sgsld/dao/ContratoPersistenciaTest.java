package sgsld.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Region;
import sgsld.modelo.Rol;
import sgsld.modelo.Usuario;
import sgsld.modelo.ZonaCorporal;
import sgsld.servicio.AutenticacionService;
import sgsld.servicio.CategoriaService;
import sgsld.servicio.JugadorService;
import sgsld.servicio.KinesiologoService;
import sgsld.util.HashContrasena;
import sgsld.util.SesionUsuario;
import sgsld.util.ValidacionException;

/**
 * Contrato de la persistencia: las mismas pruebas de DAO y servicios se
 * ejecutan con los DAO en memoria (siempre) y con MySQL (etiqueta
 * {@code mysql}). Cada subclase indica cómo obtener la fábrica.
 * <p>
 * Las pruebas que modifican datos usan DNI, matrículas y nombres propios, de
 * modo que no se afectan entre sí aunque compartan la base.
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
}
