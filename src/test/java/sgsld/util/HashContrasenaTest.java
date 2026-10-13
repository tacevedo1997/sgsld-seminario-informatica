package sgsld.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Pruebas de {@link HashContrasena}, incluida la compatibilidad con los hashes
 * del script de la base de datos de la entrega 2.
 */
@DisplayName("HashContrasena · PBKDF2-HMAC-SHA256")
class HashContrasenaTest {

    private static final String CONTRASENA_LOTE = "Sgsld2026!";

    @ParameterizedTest(name = "Hash del script verifica con Sgsld2026! · {0}")
    @DisplayName("Compatibilidad con los hashes de sgsld_script.sql")
    @ValueSource(strings = {
        "pbkdf2_sha256$600000$9tMaGR8tPF6YEDLEVayIyw==$LZeYsYJMJVj/3d6krUtBIQSx6tWji/r21RIlOAEdnJc=",
        "pbkdf2_sha256$600000$26N3kgXnzRtg4Rwns5UpCg==$FSrI8YsD/HSvjMnXMkMJB4OT9D2educQw/LVTVkHqJw=",
        "pbkdf2_sha256$600000$zzhls+mk8j3ECd3FpaymwA==$r2jHjIAQ4SLnyPBe9r1FbFnVvbo4R05+z0cPHJZ4VNY=",
        "pbkdf2_sha256$600000$nAQW/lf9vCWoJjBOB3UjQA==$y4j/gFkevLA1cR2HKsCKuAAycral/gvBPuzwDVANuYs="
    })
    void verificaHashesDelScript(String hash) {
        assertTrue(HashContrasena.verificar(CONTRASENA_LOTE, hash));
    }

    @Test
    @DisplayName("Un hash generado verifica con su contraseña y no con otra")
    void generaYVerifica() {
        String hash = HashContrasena.generar("ClaveSegura1".toCharArray());
        assertTrue(hash.startsWith("pbkdf2_sha256$600000$"));
        assertTrue(hash.length() <= 120, "Debe caber en VARCHAR(120)");
        assertTrue(HashContrasena.verificar("ClaveSegura1", hash));
        assertFalse(HashContrasena.verificar("ClaveSegura2", hash));
    }

    @Test
    @DisplayName("Dos hashes de la misma contraseña difieren por la sal")
    void salAleatoria() {
        String primero = HashContrasena.generar(CONTRASENA_LOTE.toCharArray());
        String segundo = HashContrasena.generar(CONTRASENA_LOTE.toCharArray());
        assertFalse(primero.equals(segundo));
    }

    @Test
    @DisplayName("Un hash con formato inválido no verifica")
    void formatoInvalido() {
        assertFalse(HashContrasena.verificar(CONTRASENA_LOTE, "texto-sin-formato"));
        assertFalse(HashContrasena.verificar(CONTRASENA_LOTE, "pbkdf2_sha256$abc$AAAA$AAAA"));
        assertFalse(HashContrasena.verificar(CONTRASENA_LOTE, "pbkdf2_sha256$600000$%%%$AAAA"));
        assertFalse(HashContrasena.verificar(CONTRASENA_LOTE, null));
    }
}
