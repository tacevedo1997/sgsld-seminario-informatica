package sgsld.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas con PBKDF2-HMAC-SHA256 (RNF08), solo con el JDK.
 * <p>
 * Formato almacenado, compatible con la columna {@code usuario.hash_contrasena}
 * ({@code VARCHAR(120)}):
 * {@code pbkdf2_sha256$600000$<sal en Base64>$<hash en Base64>}, con sal
 * aleatoria de 16 bytes y clave derivada de 256 bits. La comparación es en
 * tiempo constante para no filtrar información por el tiempo de respuesta.
 */
public final class HashContrasena {

    /** Identificador del algoritmo en el texto almacenado. */
    public static final String PREFIJO = "pbkdf2_sha256";
    /** Iteraciones de PBKDF2. */
    public static final int ITERACIONES = 600_000;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String SEPARADOR = "$";
    private static final int BYTES_SAL = 16;
    private static final int BITS_CLAVE = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private HashContrasena() {
    }

    /**
     * Genera el hash de una contraseña con una sal aleatoria nueva.
     *
     * @param contrasena contraseña en claro (el llamador debe limpiarla después)
     * @return el texto {@code pbkdf2_sha256$600000$<sal>$<hash>}
     */
    public static String generar(char[] contrasena) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(contrasena, sal, ITERACIONES, BITS_CLAVE);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIJO + SEPARADOR + ITERACIONES + SEPARADOR
                + base64.encodeToString(sal) + SEPARADOR + base64.encodeToString(hash);
    }

    /**
     * Verifica una contraseña contra un hash almacenado.
     *
     * @param contrasena     contraseña ingresada
     * @param hashAlmacenado texto con el formato de {@link #generar(char[])}
     * @return {@code true} si la contraseña corresponde al hash; {@code false}
     *         si no corresponde o si el texto almacenado tiene un formato inválido
     */
    public static boolean verificar(char[] contrasena, String hashAlmacenado) {
        if (contrasena == null || hashAlmacenado == null) {
            return false;
        }
        String[] partes = hashAlmacenado.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (iteraciones <= 0 || esperado.length == 0) {
                return false;
            }
            byte[] calculado = derivar(contrasena, sal, iteraciones, esperado.length * Byte.SIZE);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            // Número o Base64 inválidos: el hash almacenado está corrupto.
            return false;
        }
    }

    /**
     * Variante de {@link #verificar(char[], String)} que recibe un {@code String}
     * (útil en pruebas; la interfaz usa {@code char[]} para poder limpiarlo).
     *
     * @param contrasena     contraseña ingresada
     * @param hashAlmacenado hash almacenado
     * @return {@code true} si la contraseña corresponde al hash
     */
    public static boolean verificar(String contrasena, String hashAlmacenado) {
        return contrasena != null && verificar(contrasena.toCharArray(), hashAlmacenado);
    }

    private static byte[] derivar(char[] contrasena, byte[] sal, int iteraciones, int bitsClave) {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena, sal, iteraciones, bitsClave);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
