package sgsld.dao;

import org.junit.jupiter.api.DisplayName;

import sgsld.dao.memoria.DatosDePrueba;
import sgsld.dao.memoria.FabricaDAOMemoria;

/** Contrato de reportes y estado actual con los DAO en memoria (se ejecuta siempre). */
@DisplayName("Contrato de reportes y estado actual · memoria")
class ContratoReportesMemoriaTest extends ContratoReportesTest {

    private final FabricaDAOMemoria fabrica = new FabricaDAOMemoria();

    ContratoReportesMemoriaTest() {
        DatosDePrueba.cargar(fabrica);
    }

    @Override
    protected FabricaDAO fabrica() {
        return fabrica;
    }
}