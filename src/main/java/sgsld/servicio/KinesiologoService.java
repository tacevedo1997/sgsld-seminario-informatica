package sgsld.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import sgsld.dao.KinesiologoDAO;
import sgsld.dao.UsuarioDAO;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Rol;
import sgsld.modelo.Usuario;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.Algoritmos;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.HashContrasena;
import sgsld.util.PersistenciaException;
import sgsld.util.SesionUsuario;
import sgsld.util.Texto;
import sgsld.util.Validaciones;
import sgsld.util.ValidacionException;

/**
 * Gestión de kinesiólogos (CU-03). Altas y modificaciones: Coordinador.
 * Listado y búsqueda por usuario: también Kinesiólogo, que los necesita para
 * elegir al responsable de un tratamiento (CU-05).
 */
public class KinesiologoService {

    /** Longitud mínima de la contraseña de un usuario nuevo. */
    public static final int MIN_CONTRASENA = 8;

    private final KinesiologoDAO kinesiologos;
    private final UsuarioDAO usuarios;

    /**
     * Crea el servicio.
     *
     * @param kinesiologos DAO de kinesiólogos
     * @param usuarios     DAO de usuarios
     */
    public KinesiologoService(KinesiologoDAO kinesiologos, UsuarioDAO usuarios) {
        this.kinesiologos = kinesiologos;
        this.usuarios = usuarios;
    }

    /**
     * Lista los kinesiólogos ordenados por apellido y nombre.
     *
     * @return los kinesiólogos
     * @throws AccesoDenegadoException si el rol no es Coordinador ni Kinesiólogo
     */
    public List<Kinesiologo> listar() throws AccesoDenegadoException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR, Rol.KINESIOLOGO);
        List<Kinesiologo> lista = kinesiologos.listarTodos();
        Algoritmos.ordenarPorInsercion(lista, (a, b) -> Texto.comparar(a.getNombreCompleto(), b.getNombreCompleto()));
        return lista;
    }

    /**
     * Busca un kinesiólogo por su número.
     *
     * @param id identificador
     * @return el kinesiólogo
     * @throws AccesoDenegadoException      si el rol no es Coordinador
     * @throws EntidadNoEncontradaException si no existe
     */
    public Kinesiologo buscarPorId(int id)
            throws AccesoDenegadoException, EntidadNoEncontradaException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        return kinesiologos.buscarPorId(id).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró el kinesiólogo N° " + id + "."));
    }

    /**
     * Busca el kinesiólogo asociado a un usuario (por ejemplo, el de la sesión).
     *
     * @param idUsuario identificador del usuario
     * @return el kinesiólogo, o vacío si el usuario no corresponde a ninguno
     * @throws AccesoDenegadoException si el rol no es Coordinador ni Kinesiólogo
     */
    public Optional<Kinesiologo> buscarPorUsuario(int idUsuario)
            throws AccesoDenegadoException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR, Rol.KINESIOLOGO);
        return kinesiologos.buscarPorUsuario(idUsuario);
    }

    /**
     * Registra un kinesiólogo sin usuario del sistema.
     *
     * @param kinesiologo kinesiólogo nuevo
     * @return el identificador asignado
     * @throws AccesoDenegadoException si el rol no es Coordinador
     * @throws ValidacionException     con todos los errores de los datos
     */
    public int registrar(Kinesiologo kinesiologo)
            throws AccesoDenegadoException, ValidacionException, PersistenciaException {
        return registrar(kinesiologo, null, null);
    }

    /**
     * Registra un kinesiólogo y, si se indica un nombre de usuario, crea su
     * usuario con rol Kinesiólogo. La contraseña se guarda solo como hash; el
     * llamador debe limpiar el arreglo después.
     *
     * @param kinesiologo   kinesiólogo nuevo
     * @param nombreUsuario nombre de usuario a crear, o {@code null} para no crearlo
     * @param contrasena    contraseña del usuario (al menos 8 caracteres)
     * @return el identificador asignado al kinesiólogo
     * @throws AccesoDenegadoException si el rol no es Coordinador
     * @throws ValidacionException     con todos los errores de los datos
     */
    public int registrar(Kinesiologo kinesiologo, String nombreUsuario, char[] contrasena)
            throws AccesoDenegadoException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        List<String> errores = validar(kinesiologo);
        if (nombreUsuario != null) {
            validarUsuarioNuevo(nombreUsuario, contrasena, errores);
        }
        ValidacionException.lanzarSiHayErrores(errores);
        if (nombreUsuario != null) {
            Usuario usuario = new Usuario(nombreUsuario.trim(), HashContrasena.generar(contrasena), Rol.KINESIOLOGO);
            usuarios.insertar(usuario);
            kinesiologo.setUsuario(usuario);
        }
        return kinesiologos.insertar(kinesiologo);
    }

    /**
     * Modifica nombre, apellido y matrícula de un kinesiólogo. Valida los
     * cambios y, solo si son correctos, los copia sobre el registrado.
     *
     * @param cambios kinesiólogo con el id del registrado y los datos nuevos
     * @throws AccesoDenegadoException      si el rol no es Coordinador
     * @throws EntidadNoEncontradaException si el kinesiólogo no existe
     * @throws ValidacionException          con todos los errores de los datos
     */
    public void modificar(Kinesiologo cambios)
            throws AccesoDenegadoException, EntidadNoEncontradaException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        Kinesiologo actual = kinesiologos.buscarPorId(cambios.getId()).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró el kinesiólogo N° " + cambios.getId() + "."));
        ValidacionException.lanzarSiHayErrores(validar(cambios));
        actual.setNombre(cambios.getNombre());
        actual.setApellido(cambios.getApellido());
        actual.setMatricula(cambios.getMatricula());
        kinesiologos.actualizar(actual);
    }

    private List<String> validar(Kinesiologo kinesiologo) throws PersistenciaException {
        List<String> errores = new ArrayList<>(kinesiologo.validar());
        Optional<Kinesiologo> mismaMatricula = kinesiologos.buscarPorMatricula(kinesiologo.getMatricula());
        if (mismaMatricula.isPresent() && mismaMatricula.get().getId() != kinesiologo.getId()) {
            errores.add("Ya existe un kinesiólogo con matrícula " + mismaMatricula.get().getMatricula() + ".");
        }
        return errores;
    }

    private void validarUsuarioNuevo(String nombreUsuario, char[] contrasena, List<String> errores)

            throws PersistenciaException {
        Validaciones.texto(errores, nombreUsuario, "nombre de usuario", true, Usuario.MAX_NOMBRE_USUARIO);
        if (!Texto.estaVacio(nombreUsuario) && usuarios.buscarPorNombreUsuario(nombreUsuario.trim()).isPresent()) {
            errores.add("Ya existe un usuario con el nombre " + nombreUsuario.trim() + ".");
        }
        if (contrasena == null || contrasena.length < MIN_CONTRASENA) {
            errores.add("La contraseña debe tener al menos " + MIN_CONTRASENA + " caracteres.");
        }
    }
}
