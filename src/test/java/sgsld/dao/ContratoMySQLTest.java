package sgsld.dao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import sgsld.dao.mysql.BaseDePruebasMySQL;
import sgsld.dao.mysql.ConexionBD;
import sgsld.dao.mysql.FabricaPruebaMySQL;

/**
 * Contrato de la persistencia con MySQL, sobre la base {@code sgsld_test}
 * recreada desde {@code sgsld_script.sql} antes de la clase
 * ({@code mvn test -Dgroups=mysql}).
 */
@Tag("mysql")
@DisplayName("Contrato de persistencia · MySQL (sgsld_test)")
class ContratoMySQLTest extends ContratoPersistenciaTest {

    private static ConexionBD conexion;

    @BeforeAll
    static void recrearBase() throws Exception {
        conexion = BaseDePruebasMySQL.recrear();
    }

    @Override
    protected FabricaDAO fabrica() {
        return new FabricaPruebaMySQL(conexion);
    }

    /** Una fábrica nueva no comparte objetos con la anterior: lo releído viene de la base. */
    @Override
    protected FabricaDAO fabricaNueva() {
        return new FabricaPruebaMySQL(conexion);
    }
}
