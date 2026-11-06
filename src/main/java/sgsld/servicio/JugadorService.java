package sgsld.servicio;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import sgsld.dao.CategoriaDAO;
import sgsld.dao.JugadorDAO;
import sgsld.modelo.Categoria;
import sgsld.modelo.Jugador;
import sgsld.modelo.Rol;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.Algoritmos;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.PersistenciaException;
import sgsld.util.SesionUsuario;
import sgsld.util.Texto;
import sgsld.util.ValidacionException;

/**
 * Gestión de jugadores (CU-01). Altas, modificaciones y bajas: Coordinador.
 * Consultas: Coordinador y Kinesiólogo.
 */
public class JugadorService {

    /** Orden de los listados: apellido y nombre, alfabético español. */
    static final Comparator<Jugador> POR_APELLIDO_Y_NOMBRE =
            Comparator.comparing(Jugador::getApellido, Texto.ordenAlfabetico())
                    .thenComparing(Jugador::getNombre, Texto.ordenAlfabetico());

    private final JugadorDAO jugadores;
    private final CategoriaDAO categorias;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param jugadores  DAO de jugadores
     * @param categorias DAO de categorías
     * @param reloj      reloj que define la fecha actual
     */
    public JugadorService(JugadorDAO jugadores, CategoriaDAO categorias, Clock reloj) {
        this.jugadores = jugadores;
        this.categorias = categorias;
        this.reloj = reloj;
    }

    /**
     * Lista los jugadores ordenados por apellido y nombre.
     *
     * @param incluirInactivos {@code true} para incluir los dados de baja
     * @return los jugadores
     * @throws AccesoDenegadoException si el rol no es Coordinador ni Kinesiólogo
     */
    public List<Jugador> listar(boolean incluirInactivos) throws AccesoDenegadoException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR, Rol.KINESIOLOGO);
        List<Jugador> lista = incluirInactivos ? jugadores.listarTodos() : jugadores.listarActivos();
        Algoritmos.ordenarPorInsercion(lista, POR_APELLIDO_Y_NOMBRE);
        return lista;
    }

    /**
     * Busca un jugador por DNI (búsqueda binaria en el índice por DNI).
     *
     * @param dni DNI exacto
     * @return el jugador
     * @throws AccesoDenegadoException      si el rol no es Coordinador ni Kinesiólogo
     * @throws EntidadNoEncontradaException si no hay un jugador con ese DNI
     */
    public Jugador buscarPorDni(String dni)
            throws AccesoDenegadoException, EntidadNoEncontradaException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR, Rol.KINESIOLOGO);
        return jugadores.buscarPorDni(dni).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró un jugador con DNI " + dni + "."));
    }

    /**
     * Busca jugadores por coincidencia parcial del apellido (búsqueda secuencial).
     *
     * @param apellido texto a buscar, sin distinguir mayúsculas ni tildes
     * @return los jugadores encontrados, ordenados; vacía si no hay coincidencias
     * @throws AccesoDenegadoException si el rol no es Coordinador ni Kinesiólogo
     */
    public List<Jugador> buscarPorApellido(String apellido) throws AccesoDenegadoException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR, Rol.KINESIOLOGO);
        if (Texto.estaVacio(apellido)) {
            return new ArrayList<>();
        }
        List<Jugador> encontrados = jugadores.buscarPorApellido(apellido);
        Algoritmos.ordenarPorInsercion(encontrados, POR_APELLIDO_Y_NOMBRE);
        return encontrados;
    }

    /**
     * Busca un jugador por su número interno.
     *
     * @param id identificador
     * @return el jugador
     * @throws AccesoDenegadoException      si el rol no es Coordinador ni Kinesiólogo
     * @throws EntidadNoEncontradaException si no existe
     */
    public Jugador buscarPorId(int id)
            throws AccesoDenegadoException, EntidadNoEncontradaException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR, Rol.KINESIOLOGO);
        return jugadores.buscarPorId(id).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró el jugador N° " + id + "."));
    }

    /**
     * Da de alta un jugador.
     *
     * @param jugador jugador nuevo
     * @return el identificador asignado
     * @throws AccesoDenegadoException si el rol no es Coordinador
     * @throws ValidacionException     con todos los errores de los datos
     */
    public int registrar(Jugador jugador)
            throws AccesoDenegadoException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        List<String> errores = new ArrayList<>();
        if (categorias.listarTodos().isEmpty()) {
            errores.add("Debe registrar al menos una categoría antes de dar de alta jugadores.");
        }
        validar(jugador, errores);
        ValidacionException.lanzarSiHayErrores(errores);
        return jugadores.insertar(jugador);
    }

    /**
     * Modifica los datos de un jugador. Valida los cambios recibidos y, solo si
     * son correctos, los copia sobre el jugador registrado.
     *
     * @param cambios jugador con el id del registrado y los datos nuevos
     * @throws AccesoDenegadoException      si el rol no es Coordinador
     * @throws EntidadNoEncontradaException si el jugador no existe
     * @throws ValidacionException          con todos los errores de los datos
     */
    public void modificar(Jugador cambios)
            throws AccesoDenegadoException, EntidadNoEncontradaException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        Jugador actual = jugadores.buscarPorId(cambios.getId()).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró el jugador N° " + cambios.getId() + "."));
        List<String> errores = new ArrayList<>();
        validar(cambios, errores);
        ValidacionException.lanzarSiHayErrores(errores);
        actual.setDni(cambios.getDni());
        actual.setNombre(cambios.getNombre());
        actual.setApellido(cambios.getApellido());
        actual.setFechaNacimiento(cambios.getFechaNacimiento());
        actual.setNumeroCamiseta(cambios.getNumeroCamiseta());
        actual.setPosicion(cambios.getPosicion());
        actual.setCategoria(cambios.getCategoria());
        jugadores.actualizar(actual);
    }

    /**
     * Baja lógica de un jugador (CU-01, 3b): deja de figurar en los listados y
     * en el estado de disponibilidad, pero conserva su historial.
     *
     * @param idJugador identificador del jugador
     * @throws AccesoDenegadoException      si el rol no es Coordinador
     * @throws EntidadNoEncontradaException si el jugador no existe
     * @throws ValidacionException          si ya estaba dado de baja
     */
    public void darDeBaja(int idJugador)
            throws AccesoDenegadoException, EntidadNoEncontradaException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        Jugador jugador = jugadores.buscarPorId(idJugador).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró el jugador N° " + idJugador + "."));
        if (!jugador.isActivo()) {
            throw new ValidacionException("El jugador ya estaba dado de baja.");
        }
        jugador.setActivo(false);
        jugadores.actualizar(jugador);
    }

    /**
     * Fecha actual según el reloj del servicio.
     *
     * @return la fecha de hoy
     */
    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private void validar(Jugador jugador, List<String> errores) throws PersistenciaException {
        errores.addAll(jugador.validar());
        if (jugador.getFechaNacimiento() != null && jugador.getFechaNacimiento().isAfter(hoy())) {
            errores.add("La fecha de nacimiento no puede ser posterior a la fecha actual.");
        }
        if (!Texto.estaVacio(jugador.getDni())) {
            Optional<Jugador> mismoDni = jugadores.buscarPorDni(jugador.getDni().trim());
            if (mismoDni.isPresent() && mismoDni.get().getId() != jugador.getId()) {
                errores.add("Ya existe un jugador con DNI " + jugador.getDni().trim() + ".");
            }
        }
        Categoria categoria = jugador.getCategoria();
        if (categoria != null && categorias.buscarPorId(categoria.getId()).isEmpty()) {
            errores.add("La categoría seleccionada no existe.");
        }
    }
}
