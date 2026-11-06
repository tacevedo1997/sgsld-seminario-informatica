package sgsld.dao.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;

import sgsld.modelo.Categoria;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.TipoLesion;
import sgsld.modelo.Usuario;
import sgsld.modelo.ZonaCorporal;

/**
 * Columnas y mapeadores compartidos por los DAO MySQL.
 * <p>
 * Las columnas se seleccionan con alias únicos ({@code categoria_nombre},
 * {@code jugador_nombre}...) para poder combinarlas en un mismo {@code JOIN}
 * sin ambigüedades. Los fragmentos son texto fijo del programa: nunca
 * contienen datos ingresados por el usuario.
 */
final class Mapeos {

    static final String COLUMNAS_CATEGORIA =
            "c.id_categoria, c.nombre AS categoria_nombre, c.edad_min, c.edad_max";

    static final String COLUMNAS_JUGADOR =
            "j.id_jugador, j.dni, j.nombre AS jugador_nombre, j.apellido AS jugador_apellido, j.fecha_nacimiento, "
            + "j.numero_camiseta, j.posicion, j.activo AS jugador_activo, " + COLUMNAS_CATEGORIA;

    static final String DESDE_JUGADOR = "jugador j JOIN categoria c ON c.id_categoria = j.id_categoria";

    static final String COLUMNAS_USUARIO =
            "u.id_usuario, u.nombre_usuario, u.hash_contrasena, u.rol, u.activo AS usuario_activo";

    static final String COLUMNAS_KINESIOLOGO =
            "k.id_kinesiologo, k.nombre AS kinesiologo_nombre, k.apellido AS kinesiologo_apellido, k.matricula, "
            + COLUMNAS_USUARIO;

    static final String DESDE_KINESIOLOGO = "kinesiologo k LEFT JOIN usuario u ON u.id_usuario = k.id_usuario";

    static final String COLUMNAS_ZONA = "z.id_zona, z.nombre AS zona_nombre, z.region";

    static final String COLUMNAS_TIPO = "tl.id_tipo, tl.nombre AS tipo_nombre";

    private Mapeos() {
    }

    static Categoria categoria(ResultSet fila) throws SQLException {
        return new Categoria(fila.getInt("id_categoria"), fila.getString("categoria_nombre"),
                fila.getInt("edad_min"), fila.getInt("edad_max"));
    }

    static Jugador jugador(ResultSet fila) throws SQLException {
        return new Jugador(fila.getInt("id_jugador"), fila.getString("dni"), fila.getString("jugador_nombre"),
                fila.getString("jugador_apellido"), AccesoDatosMySQL.fecha(fila, "fecha_nacimiento"),
                AccesoDatosMySQL.enteroOpcional(fila, "numero_camiseta"), fila.getString("posicion"),
                fila.getBoolean("jugador_activo"), categoria(fila));
    }

    static Usuario usuario(ResultSet fila) throws SQLException {
        return new Usuario(fila.getInt("id_usuario"), fila.getString("nombre_usuario"),
                fila.getString("hash_contrasena"), ValoresEnum.rol(fila.getString("rol")),
                fila.getBoolean("usuario_activo"));
    }

    /** El usuario es opcional (relación 0..1): {@code null} si el {@code LEFT JOIN} no encontró. */
    static Kinesiologo kinesiologo(ResultSet fila) throws SQLException {
        Usuario usuario = AccesoDatosMySQL.enteroOpcional(fila, "id_usuario") == null ? null : usuario(fila);
        return new Kinesiologo(fila.getInt("id_kinesiologo"), fila.getString("kinesiologo_nombre"),
                fila.getString("kinesiologo_apellido"), fila.getString("matricula"), usuario);
    }

    static ZonaCorporal zona(ResultSet fila) throws SQLException {
        return new ZonaCorporal(fila.getInt("id_zona"), fila.getString("zona_nombre"),
                ValoresEnum.region(fila.getString("region")));
    }

    static TipoLesion tipo(ResultSet fila) throws SQLException {
        return new TipoLesion(fila.getInt("id_tipo"), fila.getString("tipo_nombre"));
    }
}
