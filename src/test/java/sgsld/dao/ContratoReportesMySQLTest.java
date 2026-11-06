package sgsld.dao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import sgsld.dao.mysql.BaseDePruebasMySQL;
import sgsld.dao.mysql.ConexionBD;
import sgsld.dao.mysql.FabricaDAOMySQL;

/**
 * Contrato de reportes y estado actual con MySQL, sobre una base
 * {@code sgsld_test} recién creada que ninguna prueba de esta clase modifica.
 */
@Tag("mysql")
@DisplayName("Contrato de reportes y estado actual · MySQL (sgsld_test)")
class ContratoReportesMySQLTest extends ContratoReportesTest {

    private static ConexionBD conexion;

    @BeforeAll
    static void recrearBase() throws Exception {
        conexion = BaseDePruebasMySQL.recrear();
    }

    @Override
    protected FabricaDAO fabrica() {
        return new FabricaDAOMySQL(conexion);
    }
}