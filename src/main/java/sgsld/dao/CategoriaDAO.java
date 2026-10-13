package sgsld.dao;

import java.util.Optional;

import sgsld.modelo.Categoria;

/** Acceso a datos de categorías. */
public interface CategoriaDAO extends DAO<Categoria> {

    /**
     * Busca una categoría por nombre, sin distinguir mayúsculas ni tildes.
     *
     * @param nombre nombre de la categoría
     * @return la categoría, o vacío si no existe
     */
    Optional<Categoria> buscarPorNombre(String nombre);
}
