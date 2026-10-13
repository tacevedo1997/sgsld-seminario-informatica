package sgsld.dao.memoria;

import java.util.Optional;

import sgsld.dao.CategoriaDAO;
import sgsld.modelo.Categoria;
import sgsld.util.Texto;

/** Categorías en memoria. */
public class CategoriaDAOMemoria extends DAOMemoria<Categoria> implements CategoriaDAO {

    @Override
    public Optional<Categoria> buscarPorNombre(String nombre) {
        return buscarPrimero(categoria -> Texto.iguales(categoria.getNombre(), nombre));
    }
}
