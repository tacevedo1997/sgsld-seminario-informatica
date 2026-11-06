package sgsld.dao.mysql;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

import sgsld.modelo.Entidad;
import sgsld.util.PersistenciaException;

/**
 * Base abstracta de los DAO MySQL: centraliza el código JDBC que se repite
 * (abrir la conexión, preparar la sentencia, asignar los parámetros, recorrer
 * el resultado, cerrar todo y traducir los errores).
 * <p>
 * Cada DAO concreto solo aporta el SQL y un {@link MapeadorFila}. Todas las
 * sentencias son {@link PreparedStatement} con parámetros {@code ?}: los datos
 * ingresados nunca se concatenan al SQL. Los recursos se cierran con
 * try-with-resources y cada {@link SQLException} se traduce en una
 * {@link PersistenciaException} con {@link TraductorErrores}.
 */
public abstract class AccesoDatosMySQL {

    private final ConexionBD conexion;

    /**
     * Crea el DAO sobre una conexión configurada.
     *
     * @param conexion conexión a la base
     */
    protected AccesoDatosMySQL(ConexionBD conexion) {
        this.conexion = conexion;
    }

    /**
     * Ejecuta una consulta y convierte cada fila con el mapeador.
     *
     * @param sql        sentencia {@code SELECT} con parámetros {@code ?}
     * @param mapeador   conversión de cada fila
     * @param parametros valores de los {@code ?}, en orden
     * @param <T>        tipo de los objetos
     * @return los objetos, en el orden del resultado
     * @throws PersistenciaException si falla la consulta
     */
    protected <T> ArrayList<T> consultar(String sql, MapeadorFila<T> mapeador, Object... parametros)
            throws PersistenciaException {
        try (Connection abierta = conexion.abrir()) {
            return consultar(abierta, sql, mapeador, parametros);
        } catch (SQLException e) {
            throw TraductorErrores.traducir(e, describir(sql));
        }
    }

    /**
     * Ejecuta una consulta sobre una conexión ya abierta (por ejemplo, dentro
     * de una transacción).
     *
     * @param abierta    conexión abierta
     * @param sql        sentencia {@code SELECT} con parámetros {@code ?}
     * @param mapeador   conversión de cada fila
     * @param parametros valores de los {@code ?}, en orden
     * @param <T>        tipo de los objetos
     * @return los objetos, en el orden del resultado
     * @throws SQLException si falla la consulta
     */
    protected <T> ArrayList<T> consultar(Connection abierta, String sql, MapeadorFila<T> mapeador,
                                         Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = abierta.prepareStatement(sql)) {
            asignarParametros(sentencia, parametros);
            try (ResultSet filas = sentencia.executeQuery()) {
                ArrayList<T> resultado = new ArrayList<>();
                while (filas.next()) {
                    resultado.add(mapeador.mapear(filas));
                }
                return resultado;
            }
        }
    }

    /**
     * Ejecuta una consulta que devuelve a lo sumo una fila.
     *
     * @param sql        sentencia {@code SELECT} con parámetros {@code ?}
     * @param mapeador   conversión de la fila
     * @param parametros valores de los {@code ?}, en orden
     * @param <T>        tipo del objeto
     * @return el objeto, o vacío si no hay filas
     * @throws PersistenciaException si falla la consulta
     */
    protected <T> Optional<T> consultarUno(String sql, MapeadorFila<T> mapeador, Object... parametros)
            throws PersistenciaException {
        ArrayList<T> resultado = consultar(sql, mapeador, parametros);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    /**
     * Ejecuta un {@code INSERT} y devuelve la clave generada
     * ({@code AUTO_INCREMENT}).
     *
     * @param sql        sentencia {@code INSERT} con parámetros {@code ?}
     * @param parametros valores de los {@code ?}, en orden
     * @return el identificador generado
     * @throws PersistenciaException si falla la inserción (por ejemplo, clave duplicada)
     */
    protected int insertar(String sql, Object... parametros) throws PersistenciaException {
        try (Connection abierta = conexion.abrir()) {
            return insertar(abierta, sql, parametros);
        } catch (SQLException e) {
            throw TraductorErrores.traducir(e, describir(sql));
        }
    }

    /**
     * Ejecuta un {@code INSERT} sobre una conexión ya abierta.
     *
     * @param abierta    conexión abierta
     * @param sql        sentencia {@code INSERT} con parámetros {@code ?}
     * @param parametros valores de los {@code ?}, en orden
     * @return el identificador generado
     * @throws SQLException si falla la inserción
     */
    protected int insertar(Connection abierta, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = abierta.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            asignarParametros(sentencia, parametros);
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("La base no devolvió la clave generada");
                }
                return claves.getInt(1);
            }
        }
    }

    /**
     * Ejecuta un {@code UPDATE}.
     *
     * @param sql        sentencia con parámetros {@code ?}
     * @param parametros valores de los {@code ?}, en orden
     * @return la cantidad de filas modificadas
     * @throws PersistenciaException si falla la sentencia
     */
    protected int actualizar(String sql, Object... parametros) throws PersistenciaException {
        try (Connection abierta = conexion.abrir()) {
            return actualizar(abierta, sql, parametros);
        } catch (SQLException e) {
            throw TraductorErrores.traducir(e, describir(sql));
        }
    }

    /**
     * Ejecuta un {@code UPDATE} sobre una conexión ya abierta.
     *
     * @param abierta    conexión abierta
     * @param sql        sentencia con parámetros {@code ?}
     * @param parametros valores de los {@code ?}, en orden
     * @return la cantidad de filas modificadas
     * @throws SQLException si falla la sentencia
     */
    protected int actualizar(Connection abierta, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = abierta.prepareStatement(sql)) {
            asignarParametros(sentencia, parametros);
            return sentencia.executeUpdate();
        }
    }

    /**
     * Ejecuta varias sentencias en una transacción (<b>método plantilla</b>):
     * desactiva el {@code autoCommit}, ejecuta la operación, confirma con
     * {@code commit} y, si algo falla, deshace todo con {@code rollback}. El
     * {@code autoCommit} se restaura en el {@code finally}.
     *
     * @param operacion   qué se intenta hacer, para el mensaje de error
     * @param sentencias  sentencias de la transacción
     * @param <R>         tipo del resultado
     * @return el resultado de la operación
     * @throws PersistenciaException si falla alguna sentencia (ya se hizo el rollback)
     */
    protected <R> R enTransaccion(String operacion, OperacionTransaccional<R> sentencias)
            throws PersistenciaException {
        try (Connection abierta = conexion.abrir()) {
            boolean autoCommitOriginal = abierta.getAutoCommit();
            abierta.setAutoCommit(false);
            try {
                R resultado = sentencias.ejecutar(abierta);
                abierta.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                deshacer(abierta, e);
                throw e;
            } finally {
                abierta.setAutoCommit(autoCommitOriginal);
            }
        } catch (SQLException e) {
            throw TraductorErrores.traducir(e, operacion);
        }
    }

    /**
     * Asigna los valores a los {@code ?} de la sentencia según su tipo: el
     * único lugar donde los datos pasan a la base.
     * <ul>
     * <li>{@code null} → {@code setNull}</li>
     * <li>{@code LocalDate} → {@code java.sql.Date}</li>
     * <li>enumeración → valor de la columna {@code ENUM} ({@link ValoresEnum})</li>
     * <li>{@code String}, {@code Integer}, {@code Long}, {@code Boolean} → su setter</li>
     * </ul>
     *
     * @param sentencia  sentencia preparada
     * @param parametros valores, en el orden de los {@code ?}
     * @throws SQLException si falla una asignación
     */
    static void asignarParametros(PreparedStatement sentencia, Object... parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            int posicion = i + 1;
            Object valor = parametros[i];
            if (valor == null) {
                sentencia.setNull(posicion, Types.NULL);
            } else if (valor instanceof LocalDate fecha) {
                sentencia.setDate(posicion, Date.valueOf(fecha));
            } else if (valor instanceof Enum<?> constante) {
                sentencia.setString(posicion, ValoresEnum.aColumna(constante));
            } else if (valor instanceof String texto) {
                sentencia.setString(posicion, texto);
            } else if (valor instanceof Integer entero) {
                sentencia.setInt(posicion, entero);
            } else if (valor instanceof Long largo) {
                sentencia.setLong(posicion, largo);
            } else if (valor instanceof Boolean logico) {
                sentencia.setBoolean(posicion, logico);
            } else {
                throw new IllegalArgumentException("Tipo de parámetro no admitido: " + valor.getClass().getName());
            }
        }
    }

    /**
     * Lee un entero que admite {@code NULL}.
     *
     * @param fila    fila actual
     * @param columna nombre de la columna
     * @return el valor, o {@code null} si la columna es {@code NULL}
     * @throws SQLException si falla la lectura
     */
    protected static Integer enteroOpcional(ResultSet fila, String columna) throws SQLException {
        int valor = fila.getInt(columna);
        return fila.wasNull() ? null : valor;
    }

    /**
     * Lee una fecha ({@code DATE}) como {@code LocalDate}.
     *
     * @param fila    fila actual
     * @param columna nombre de la columna
     * @return la fecha, o {@code null} si la columna es {@code NULL}
     * @throws SQLException si falla la lectura
     */
    protected static LocalDate fecha(ResultSet fila, String columna) throws SQLException {
        return fila.getObject(columna, LocalDate.class);
    }

    /**
     * Arma el valor de un parámetro {@code LIKE} de coincidencia parcial. Los
     * comodines que escriba el usuario ({@code %}, {@code _}) se escapan para
     * que se busquen como texto. El valor viaja como parámetro, no en el SQL.
     *
     * @param texto texto buscado
     * @return por ejemplo, {@code %gom%}
     */
    protected static String patronContiene(String texto) {
        String escapado = (texto == null ? "" : texto.trim())
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }

    /**
     * Controla que la entidad sea nueva antes de insertarla: el identificador
     * lo asigna la base.
     *
     * @param entidad entidad a insertar
     * @throws IllegalArgumentException si ya tiene identificador
     */
    protected static void exigirNueva(Entidad entidad) {
        if (entidad.getId() != 0) {
            throw new IllegalArgumentException("La entidad ya tiene identificador: " + entidad.getId());
        }
    }

    /**
     * Controla que un {@code UPDATE} haya encontrado la fila.
     *
     * @param filas   filas modificadas
     * @param entidad entidad actualizada
     * @throws IllegalArgumentException si no existe la fila (igual que los DAO en memoria)
     */
    protected static void exigirExistente(int filas, Entidad entidad) {
        if (filas == 0) {
            throw new IllegalArgumentException("No existe un registro con id " + entidad.getId());
        }
    }

    /** Describe la sentencia para el mensaje de error, sin mostrar el SQL. */
    private static String describir(String sql) {
        String inicio = sql.stripLeading().toUpperCase(Locale.ROOT);
        if (inicio.startsWith("SELECT")) {
            return "consultar los datos";
        }
        if (inicio.startsWith("INSERT")) {
            return "guardar los datos";
        }
        if (inicio.startsWith("UPDATE")) {
            return "actualizar los datos";
        }
        return "acceder a los datos";
    }

    /** Deshace la transacción; si el rollback también falla, lo agrega como suprimido. */
    private static void deshacer(Connection abierta, Exception original) {
        try {
            abierta.rollback();
        } catch (SQLException e) {
            original.addSuppressed(e);
        }
    }
}
