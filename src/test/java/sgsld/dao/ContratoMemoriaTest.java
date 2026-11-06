package sgsld.dao;

import org.junit.jupiter.api.DisplayName;

import sgsld.dao.memoria.DatosDePrueba;
import sgsld.dao.memoria.FabricaDAOMemoria;

/** Contrato de la persistencia con los DAO en memoria (se ejecuta siempre). */
@DisplayName("Contrato de persistencia · memoria")
class ContratoMemoriaTest extends ContratoPersistenciaTest {

    /** JUnit crea una instancia por prueba: cada una parte del lote original. */
    private final FabricaDAOMemoria fabrica = new FabricaDAOMemoria();

    ContratoMemoriaTest() {
        DatosDePrueba.cargar(fabrica);
    }

    @Override
    protected FabricaDAO fabrica() {
        return fabrica;
    }

    /** En memoria los datos viven en la misma fábrica. */
    @Override
    protected FabricaDAO fabricaNueva() {
        return fabrica;
    }
}
