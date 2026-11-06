package sgsld.servicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import sgsld.dao.CategoriaDAO;
import sgsld.modelo.Categoria;
import sgsld.modelo.Rol;
import sgsld.util.AccesoDenegadoException;
import sgsld.util.EntidadNoEncontradaException;
import sgsld.util.PersistenciaException;
import sgsld.util.SesionUsuario;
import sgsld.util.Validaciones;
import sgsld.util.ValidacionException;

/** Gestión de categorías (CU-02). Rol: Coordinador. */
public class CategoriaService {

    private final CategoriaDAO categorias;

    /**
     * Crea el servicio.
     *
     * @param categorias DAO de categorías
     */
    public CategoriaService(CategoriaDAO categorias) {
        this.categorias = categorias;
    }

    /**
     * Lista las categorías en el orden en que fueron registradas.
     *
     * @return las categorías
     * @throws AccesoDenegadoException si el rol no es Coordinador
     */
    public List<Categoria> listar() throws AccesoDenegadoException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        return categorias.listarTodos();
    }

    /**
     * Busca una categoría por su número.
     *
     * @param id identificador
     * @return la categoría
     * @throws AccesoDenegadoException      si el rol no es Coordinador
     * @throws EntidadNoEncontradaException si no existe
     */
    public Categoria buscarPorId(int id)
            throws AccesoDenegadoException, EntidadNoEncontradaException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        return categorias.buscarPorId(id).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró la categoría N° " + id + "."));
    }

    /**
     * Registra una categoría.
     *
     * @param categoria categoría nueva
     * @return el identificador asignado
     * @throws AccesoDenegadoException si el rol no es Coordinador
     * @throws ValidacionException     con todos los errores de los datos
     */
    public int registrar(Categoria categoria)
            throws AccesoDenegadoException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        ValidacionException.lanzarSiHayErrores(validar(categoria));
        return categorias.insertar(categoria);
    }

    /**
     * Modifica una categoría. Valida los cambios y, solo si son correctos, los
     * copia sobre la categoría registrada.
     *
     * @param cambios categoría con el id de la registrada y los datos nuevos
     * @throws AccesoDenegadoException      si el rol no es Coordinador
     * @throws EntidadNoEncontradaException si la categoría no existe
     * @throws ValidacionException          con todos los errores de los datos
     */
    public void modificar(Categoria cambios)
            throws AccesoDenegadoException, EntidadNoEncontradaException, ValidacionException, PersistenciaException {
        SesionUsuario.getInstancia().exigir(Rol.COORDINADOR);
        Categoria actual = categorias.buscarPorId(cambios.getId()).orElseThrow(
                () -> new EntidadNoEncontradaException("No se encontró la categoría N° " + cambios.getId() + "."));
        ValidacionException.lanzarSiHayErrores(validar(cambios));
        actual.setNombre(cambios.getNombre());
        actual.setEdadMin(cambios.getEdadMin());
        actual.setEdadMax(cambios.getEdadMax());
        categorias.actualizar(actual);
    }

    /** Nombre obligatorio, único y de hasta 40 caracteres; rango 0 a 99 con mínimo no mayor que máximo. */
    private List<String> validar(Categoria categoria) throws PersistenciaException {
        List<String> errores = new ArrayList<>();
        Validaciones.texto(errores, categoria.getNombre(), "nombre", true, Categoria.MAX_NOMBRE);
        Optional<Categoria> mismoNombre = categorias.buscarPorNombre(categoria.getNombre());
        if (mismoNombre.isPresent() && mismoNombre.get().getId() != categoria.getId()) {
            errores.add("Ya existe una categoría con el nombre " + mismoNombre.get().getNombre() + ".");
        }
        if (fueraDeRango(categoria.getEdadMin()) || fueraDeRango(categoria.getEdadMax())) {
            errores.add("Las edades deben estar entre " + Categoria.EDAD_MINIMA + " y "
                    + Categoria.EDAD_MAXIMA + ".");
        }
        if (categoria.getEdadMin() > categoria.getEdadMax()) {
            errores.add("La edad mínima no puede ser mayor que la edad máxima.");
        }
        return errores;
    }

    private static boolean fueraDeRango(int edad) {
        return edad < Categoria.EDAD_MINIMA || edad > Categoria.EDAD_MAXIMA;
    }
}
