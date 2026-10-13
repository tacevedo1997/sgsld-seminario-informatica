package sgsld.dao;

import java.util.Optional;

import sgsld.modelo.Kinesiologo;

/** Acceso a datos de kinesiólogos. */
public interface KinesiologoDAO extends DAO<Kinesiologo> {

    /**
     * Busca un kinesiólogo por matrícula, sin distinguir mayúsculas ni tildes.
     *
     * @param matricula matrícula profesional
     * @return el kinesiólogo, o vacío si no existe
     */
    Optional<Kinesiologo> buscarPorMatricula(String matricula);

    /**
     * Busca el kinesiólogo asociado a un usuario.
     *
     * @param idUsuario identificador del usuario
     * @return el kinesiólogo, o vacío si el usuario no corresponde a ninguno
     */
    Optional<Kinesiologo> buscarPorUsuario(int idUsuario);
}
