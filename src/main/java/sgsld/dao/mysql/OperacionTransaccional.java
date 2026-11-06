package sgsld.dao.mysql;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Operación con varias sentencias que se ejecuta dentro de una transacción con
 * {@link AccesoDatosMySQL#enTransaccion(String, OperacionTransaccional)}: todas
 * usan la misma conexión y se confirman o se deshacen juntas.
 *
 * @param <R> tipo del resultado
 */
@FunctionalInterface
public interface OperacionTransaccional<R> {

    /**
     * Ejecuta las sentencias sobre la conexión de la transacción.
     *
     * @param conexion conexión con {@code autoCommit} desactivado
     * @return el resultado de la operación
     * @throws SQLException si falla una sentencia (provoca el rollback)
     */
    R ejecutar(Connection conexion) throws SQLException;
}
