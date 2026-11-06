package sgsld.dao;

import java.util.Optional;

import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Usuario;
import sgsld.util.PersistenciaException;

/** Acceso a datos de kinesiólogos. */
public interface KinesiologoDAO extends DAO<Kinesiologo> {

    /**
     * Busca un kinesiólogo por matrícula, sin distinguir mayúsculas ni tildes.
     *
     * @param matricula matrícula profesional
     * @return el kinesiólogo, o vacío si no existe
     */
    Optional<Kinesiologo> buscarPorMatricula(String matricula) throws PersistenciaException;

    /**
     * Busca el kinesiólogo asociado a un usuario.
     *
     * @param idUsuario identificador del usuario
     * @return el kinesiólogo, o vacío si el usuario no corresponde a ninguno
     */
    Optional<Kinesiologo> buscarPorUsuario(int idUsuario) throws PersistenciaException;

    /**
     * Guarda un kinesiólogo nuevo junto con su usuario nuevo: se guardan los
     * dos o ninguno. Asigna los identificadores y asocia el usuario al
     * kinesiólogo.
     *
     * @param kinesiologo kinesiólogo nuevo
     * @param usuario     usuario nuevo, con rol Kinesiólogo
     * @return el identificador asignado al kinesiólogo
     */
    int insertarConUsuario(Kinesiologo kinesiologo, Usuario usuario) throws PersistenciaException;
}
