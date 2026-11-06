package sgsld.util;

/**
 * Indica que no se pudo leer o guardar en el almacenamiento (por ejemplo, la
 * base MySQL no responde o rechaza la operación).
 * <p>
 * El mensaje está pensado para el usuario: nunca contiene SQL ni trazas. Los
 * detalles técnicos (SQLState, código, traza) quedan en la causa encadenada y
 * en el log técnico. El {@link Motivo} permite que los servicios reaccionen,
 * por ejemplo traduciendo una clave duplicada en un error de validación.
 */
public class PersistenciaException extends SgsldException {

    private static final long serialVersionUID = 1L;

    /** Clasificación del problema de persistencia. */
    public enum Motivo {
        /** No hay comunicación con el servidor. */
        CONEXION,
        /** Usuario o clave incorrectos, o permiso insuficiente. */
        ACCESO_DENEGADO,
        /** La base indicada en la configuración no existe. */
        BASE_INEXISTENTE,
        /** No se pudo establecer la conexión segura (TLS). */
        TLS,
        /** Se violó un índice único (error 1062). */
        CLAVE_DUPLICADA,
        /** Se violó una clave foránea (errores 1451 y 1452). */
        REFERENCIA,
        /** Se violó una restricción {@code CHECK} (error 3819). */
        RESTRICCION,
        /** La operación superó el tiempo de espera. */
        TIEMPO_AGOTADO,
        /** Otro puesto modificó los datos entre la lectura y el guardado. */
        CONFLICTO,
        /** Falta el archivo de configuración o es inválido. */
        CONFIGURACION,
        /** Cualquier otro error. */
        OTRO
    }

    private final Motivo motivo;
    private final String restriccion;

    /**
     * Crea la excepción.
     *
     * @param motivo      clasificación del problema
     * @param mensaje     mensaje claro para el usuario
     * @param restriccion índice o restricción violada (por ejemplo,
     *                    {@code uq_jugador_dni}), o {@code null} si no corresponde
     * @param causa       excepción original (por ejemplo, la {@code SQLException})
     */
    public PersistenciaException(Motivo motivo, String mensaje, String restriccion, Throwable causa) {
        super(mensaje, causa);
        this.motivo = motivo;
        this.restriccion = restriccion;
    }

    /**
     * Crea la excepción sin restricción asociada.
     *
     * @param motivo  clasificación del problema
     * @param mensaje mensaje claro para el usuario
     * @param causa   excepción original, o {@code null}
     */
    public PersistenciaException(Motivo motivo, String mensaje, Throwable causa) {
        this(motivo, mensaje, null, causa);
    }

    public Motivo getMotivo() {
        return motivo;
    }

    /**
     * Devuelve el índice o la restricción violada, si se conoce.
     *
     * @return el nombre (por ejemplo, {@code uq_tratamiento_en_curso}) o {@code null}
     */
    public String getRestriccion() {
        return restriccion;
    }

    /**
     * Indica si el problema es una clave duplicada (índice único).
     *
     * @return {@code true} si el motivo es {@link Motivo#CLAVE_DUPLICADA}
     */
    public boolean esClaveDuplicada() {
        return motivo == Motivo.CLAVE_DUPLICADA;
    }
}
