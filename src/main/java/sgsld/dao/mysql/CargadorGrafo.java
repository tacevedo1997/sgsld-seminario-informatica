package sgsld.dao.mysql;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sgsld.modelo.EstadoTratamiento;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Lesion;
import sgsld.modelo.Tratamiento;
import sgsld.util.Algoritmos;
import sgsld.util.PersistenciaException;

/**
 * Reconstruye el grafo clínico lesión → tratamientos → evoluciones a partir
 * de la base, con <b>tres consultas</b> (lesiones, tratamientos y evoluciones
 * de las lesiones elegidas) en lugar de una consulta por lesión (N+1).
 * <p>
 * Respeta el diagrama de estados del tratamiento: cada uno se crea en curso,
 * recibe sus evoluciones y, si en la base figura finalizado, se finaliza con su
 * fecha de fin. Recién entonces se asocia a su lesión, que admite a lo sumo un
 * tratamiento en curso. Jugadores y kinesiólogos repetidos se comparten.
 */
final class CargadorGrafo extends AccesoDatosMySQL {

    private static final String LESIONES = "SELECT l.id_lesion, l.gravedad, l.fecha AS lesion_fecha, l.descripcion, "
            + Mapeos.COLUMNAS_JUGADOR + ", " + Mapeos.COLUMNAS_ZONA + ", " + Mapeos.COLUMNAS_TIPO
            + " FROM lesion l"
            + " JOIN jugador j ON j.id_jugador = l.id_jugador"
            + " JOIN categoria c ON c.id_categoria = j.id_categoria"
            + " JOIN zona_corporal z ON z.id_zona = l.id_zona"
            + " JOIN tipo_lesion tl ON tl.id_tipo = l.id_tipo"
            + " WHERE ";

    private static final String TRATAMIENTOS = "SELECT t.id_tratamiento, t.id_lesion, t.fecha_inicio, t.fecha_fin, "
            + "t.tipo AS tratamiento_tipo, t.estado, t.observaciones, " + Mapeos.COLUMNAS_KINESIOLOGO
            + " FROM tratamiento t"
            + " JOIN kinesiologo k ON k.id_kinesiologo = t.id_kinesiologo"
            + " LEFT JOIN usuario u ON u.id_usuario = k.id_usuario"
            + " WHERE t.id_lesion IN (SELECT l.id_lesion FROM lesion l WHERE ";

    private static final String EVOLUCIONES = "SELECT " + Mapeos.COLUMNAS_EVOLUCION
            + " FROM evolucion e JOIN tratamiento t ON t.id_tratamiento = e.id_tratamiento"
            + " WHERE t.id_lesion IN (SELECT l.id_lesion FROM lesion l WHERE ";

    /** Tratamiento leído, con el estado y la fecha de fin que se aplican al final. */
    private record TratamientoLeido(Tratamiento tratamiento, EstadoTratamiento estado, LocalDate fechaFin) {
    }

    /** Evolución leída con el tratamiento al que pertenece. */
    private record EvolucionLeida(int idTratamiento, Evolucion evolucion) {
    }

    CargadorGrafo(ConexionBD conexion) {
        super(conexion);
    }

    /**
     * Carga las lesiones que cumplen la condición, con su grafo completo.
     *
     * @param conEvoluciones {@code false} para omitir las evoluciones (reportes)
     * @param condicion      condición SQL fija del programa sobre el alias {@code l}
     *                       (por ejemplo, {@code "l.id_jugador = ?"}); nunca datos del usuario
     * @param parametros     valores de los {@code ?} de la condición
     * @return las lesiones, ordenadas por número
     * @throws PersistenciaException si falla alguna consulta
     */
    List<Lesion> cargar(boolean conEvoluciones, String condicion, Object... parametros)
            throws PersistenciaException {
        Map<Integer, Jugador> jugadores = new HashMap<>();
        List<Lesion> lesiones = consultar(LESIONES + condicion + " ORDER BY l.id_lesion", fila -> {
            Jugador jugador = jugadores.get(fila.getInt("id_jugador"));
            if (jugador == null) {
                jugador = Mapeos.jugador(fila);
                jugadores.put(jugador.getId(), jugador);
            }
            return new Lesion(fila.getInt("id_lesion"), jugador, Mapeos.tipo(fila), Mapeos.zona(fila),
                    ValoresEnum.gravedad(fila.getString("gravedad")), fecha(fila, "lesion_fecha"),
                    fila.getString("descripcion"));
        }, parametros);
        if (lesiones.isEmpty()) {
            return lesiones;
        }
        Map<Integer, Lesion> lesionPorId = new HashMap<>();
        for (Lesion lesion : lesiones) {
            lesionPorId.put(lesion.getId(), lesion);
        }

        Map<Integer, Kinesiologo> kinesiologos = new HashMap<>();
        List<TratamientoLeido> leidos = consultar(TRATAMIENTOS + condicion + ") ORDER BY t.id_tratamiento", fila -> {
            Kinesiologo kinesiologo = kinesiologos.get(fila.getInt("id_kinesiologo"));
            if (kinesiologo == null) {
                kinesiologo = Mapeos.kinesiologo(fila);
                kinesiologos.put(kinesiologo.getId(), kinesiologo);
            }
            Tratamiento tratamiento = new Tratamiento(fila.getInt("id_tratamiento"),
                    lesionPorId.get(fila.getInt("id_lesion")), kinesiologo, fecha(fila, "fecha_inicio"),
                    fila.getString("tratamiento_tipo"), fila.getString("observaciones"));
            return new TratamientoLeido(tratamiento, ValoresEnum.estadoTratamiento(fila.getString("estado")),
                    fecha(fila, "fecha_fin"));
        }, parametros);

        if (conEvoluciones) {
            Map<Integer, Tratamiento> tratamientoPorId = new HashMap<>();
            for (TratamientoLeido leido : leidos) {
                tratamientoPorId.put(leido.tratamiento().getId(), leido.tratamiento());
            }
            List<EvolucionLeida> evoluciones = consultar(EVOLUCIONES + condicion
                    + ") ORDER BY e.fecha, e.id_evolucion",
                    fila -> new EvolucionLeida(fila.getInt("id_tratamiento"), Mapeos.evolucion(fila)), parametros);
            for (EvolucionLeida leida : evoluciones) {
                tratamientoPorId.get(leida.idTratamiento()).agregarEvolucion(leida.evolucion());
            }
        }

        for (TratamientoLeido leido : leidos) {
            if (leido.estado() == EstadoTratamiento.FINALIZADO) {
                leido.tratamiento().finalizar(leido.fechaFin());
            }
            leido.tratamiento().getLesion().agregarTratamiento(leido.tratamiento());
        }
        return new ArrayList<>(lesiones);
    }

    /**
     * Devuelve los tratamientos de un grafo, ordenados por número.
     *
     * @param lesiones lesiones cargadas
     * @return sus tratamientos
     */
    static List<Tratamiento> tratamientos(List<Lesion> lesiones) {
        ArrayList<Tratamiento> tratamientos = new ArrayList<>();
        for (Lesion lesion : lesiones) {
            tratamientos.addAll(lesion.getTratamientos());
        }
        Algoritmos.ordenarPorInsercion(tratamientos, Comparator.comparingInt(Tratamiento::getId));
        return tratamientos;
    }
}
