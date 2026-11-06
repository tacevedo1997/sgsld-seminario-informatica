package sgsld.dao.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Convierte la fila actual de un {@link ResultSet} en un objeto del modelo.
 * Es una interfaz funcional: cada DAO la implementa con una expresión lambda o
 * una referencia a método.
 *
 * @param <T> tipo del objeto que se construye
 */
@FunctionalInterface
public interface MapeadorFila<T> {

    /**
     * Construye el objeto a partir de la fila actual (no avanza el cursor).
     *
     * @param fila resultado posicionado en la fila a leer
     * @return el objeto construido
     * @throws SQLException si falla la lectura de una columna
     */
    T mapear(ResultSet fila) throws SQLException;
}
