package sgsld.dao.memoria;

import java.time.LocalDate;

import sgsld.modelo.Categoria;
import sgsld.modelo.Evolucion;
import sgsld.modelo.Gravedad;
import sgsld.modelo.Jugador;
import sgsld.modelo.Kinesiologo;
import sgsld.modelo.Lesion;
import sgsld.modelo.Region;
import sgsld.modelo.ResultadoEvolucion;
import sgsld.modelo.Rol;
import sgsld.modelo.TipoLesion;
import sgsld.modelo.Tratamiento;
import sgsld.modelo.Usuario;
import sgsld.modelo.ZonaCorporal;

/**
 * Lote de prueba de la sección 14.5 de la entrega 2, idéntico a los
 * {@code INSERT} de {@code sgsld_script.sql} (mismos identificadores, datos y
 * hashes de contraseña). Se carga en los DAO de una {@link FabricaDAOMemoria}.
 * <p>
 * Los tratamientos finalizados se reconstruyen como en el diagrama de estados:
 * se crean en curso, se les agregan sus evoluciones y luego se finalizan.
 */
public final class DatosDePrueba {

    // Hashes completos del script (contraseña de todos: la del README).
    private static final String HASH_MLOPEZ =
            "pbkdf2_sha256$600000$9tMaGR8tPF6YEDLEVayIyw==$LZeYsYJMJVj/3d6krUtBIQSx6tWji/r21RIlOAEdnJc=";
    private static final String HASH_LFERNANDEZ =
            "pbkdf2_sha256$600000$26N3kgXnzRtg4Rwns5UpCg==$FSrI8YsD/HSvjMnXMkMJB4OT9D2educQw/LVTVkHqJw=";
    private static final String HASH_ASOSA =
            "pbkdf2_sha256$600000$zzhls+mk8j3ECd3FpaymwA==$r2jHjIAQ4SLnyPBe9r1FbFnVvbo4R05+z0cPHJZ4VNY=";
    private static final String HASH_CTECNICO =
            "pbkdf2_sha256$600000$nAQW/lf9vCWoJjBOB3UjQA==$y4j/gFkevLA1cR2HKsCKuAAycral/gvBPuzwDVANuYs=";

    private DatosDePrueba() {
    }

    /**
     * Carga el lote completo en los DAO de la fábrica.
     *
     * @param fabrica fábrica con los DAO vacíos
     */
    public static void cargar(FabricaDAOMemoria fabrica) {
        Categoria primera = new Categoria(1, "Primera", 18, 40);
        Categoria reserva = new Categoria(2, "Reserva", 18, 23);
        Categoria sub17 = new Categoria(3, "Sub-17", 16, 17);
        Categoria sub15 = new Categoria(4, "Sub-15", 14, 15);
        for (Categoria categoria : new Categoria[] {primera, reserva, sub17, sub15}) {
            fabrica.getCategoriaDAO().insertar(categoria);
        }

        Usuario lfernandez = new Usuario(2, "lfernandez", HASH_LFERNANDEZ, Rol.KINESIOLOGO, true);
        Usuario asosa = new Usuario(3, "asosa", HASH_ASOSA, Rol.KINESIOLOGO, true);
        fabrica.getUsuarioDAO().insertar(new Usuario(1, "mlopez", HASH_MLOPEZ, Rol.COORDINADOR, true));
        fabrica.getUsuarioDAO().insertar(lfernandez);
        fabrica.getUsuarioDAO().insertar(asosa);
        fabrica.getUsuarioDAO().insertar(new Usuario(4, "ctecnico", HASH_CTECNICO, Rol.CUERPO_TECNICO, true));

        Kinesiologo fernandez = new Kinesiologo(1, "Laura", "Fernández", "MP-10234", lfernandez);
        Kinesiologo sosa = new Kinesiologo(2, "Andrés", "Sosa", "MP-11872", asosa);
        fabrica.getKinesiologoDAO().insertar(fernandez);
        fabrica.getKinesiologoDAO().insertar(sosa);
        fabrica.getKinesiologoDAO().insertar(new Kinesiologo(3, "Carlos", "Ruiz", "MP-12950", null));

        ZonaCorporal isquiotibiales = new ZonaCorporal(1, "Isquiotibiales", Region.MIEMBRO_INFERIOR);
        ZonaCorporal cuadriceps = new ZonaCorporal(2, "Cuádriceps", Region.MIEMBRO_INFERIOR);
        ZonaCorporal tobillo = new ZonaCorporal(3, "Tobillo", Region.MIEMBRO_INFERIOR);
        ZonaCorporal rodilla = new ZonaCorporal(4, "Rodilla", Region.MIEMBRO_INFERIOR);
        ZonaCorporal aductores = new ZonaCorporal(5, "Aductores", Region.MIEMBRO_INFERIOR);
        ZonaCorporal hombro = new ZonaCorporal(6, "Hombro", Region.MIEMBRO_SUPERIOR);
        ZonaCorporal lumbar = new ZonaCorporal(7, "Zona lumbar", Region.TRONCO);
        for (ZonaCorporal zona : new ZonaCorporal[] {
            isquiotibiales, cuadriceps, tobillo, rodilla, aductores, hombro, lumbar}) {
            fabrica.getZonaCorporalDAO().insertar(zona);
        }

        TipoLesion muscular = new TipoLesion(1, "Muscular");
        TipoLesion ligamentaria = new TipoLesion(2, "Ligamentaria");
        TipoLesion contusion = new TipoLesion(5, "Contusión");
        for (TipoLesion tipo : new TipoLesion[] {
            muscular, ligamentaria, new TipoLesion(3, "Tendinosa"), new TipoLesion(4, "Ósea"), contusion}) {
            fabrica.getTipoLesionDAO().insertar(tipo);
        }

        Jugador perez = jugador(fabrica, 1, "41236587", "Juan", "Pérez", "1998-03-14", 9, "Delantero", primera);
        Jugador gomez = jugador(fabrica, 2, "42587634", "Pedro", "Gómez", "2000-07-22", 5, "Mediocampista", primera);
        Jugador diaz = jugador(fabrica, 3, "45120398", "Luis", "Díaz", "2004-02-10", 4, "Defensor", reserva);
        Jugador romero = jugador(fabrica, 4, "49365120", "Matías", "Romero", "2009-04-18", 10, "Mediocampista", sub17);
        Jugador herrera = jugador(fabrica, 5, "50214786", "Tomás", "Herrera", "2010-01-25", 3, "Defensor", sub17);
        Jugador acosta = jugador(fabrica, 6, "46078912", "Franco", "Acosta", "2005-06-30", 11, "Delantero", reserva);
        Jugador castro = jugador(fabrica, 7, "51347025", "Nicolás", "Castro", "2011-09-02", 7, "Delantero", sub15);
        jugador(fabrica, 8, "39874521", "Lucas", "Molina", "1996-11-05", 1, "Arquero", primera);

        Lesion l1 = lesion(fabrica, 1, perez, ligamentaria, tobillo, Gravedad.MODERADA, "2025-09-20",
                "Esguince de tobillo derecho en entrenamiento");
        Lesion l2 = lesion(fabrica, 2, gomez, muscular, isquiotibiales, Gravedad.MODERADA, "2026-01-10",
                "Distensión en isquiotibiales durante un sprint");
        Lesion l3 = lesion(fabrica, 3, acosta, muscular, isquiotibiales, Gravedad.LEVE, "2025-11-02",
                "Molestia en isquiotibiales tras el partido");
        Lesion l4 = lesion(fabrica, 4, romero, muscular, cuadriceps, Gravedad.LEVE, "2026-03-05",
                "Sobrecarga en cuádriceps");
        Lesion l5 = lesion(fabrica, 5, diaz, contusion, rodilla, Gravedad.LEVE, "2026-04-18",
                "Golpe en la rodilla en un choque");
        Lesion l6 = lesion(fabrica, 6, herrera, muscular, aductores, Gravedad.MODERADA, "2026-05-12",
                "Distensión de aductores");
        Lesion l7 = lesion(fabrica, 7, gomez, muscular, isquiotibiales, Gravedad.GRAVE, "2026-06-15",
                "Dolor agudo en sprint durante el partido");
        Lesion l8 = lesion(fabrica, 8, acosta, muscular, isquiotibiales, Gravedad.MODERADA, "2026-07-01",
                "Distensión en isquiotibiales en entrenamiento");
        Lesion l9 = lesion(fabrica, 9, herrera, ligamentaria, tobillo, Gravedad.MODERADA, "2026-08-02",
                "Esguince de tobillo izquierdo");
        Lesion l10 = lesion(fabrica, 10, castro, contusion, hombro, Gravedad.LEVE, "2026-08-20",
                "Golpe en el hombro por caída");

        Tratamiento t1 = tratamiento(fabrica, 1, l1, fernandez, "2025-09-21", "Fisioterapia y propiocepción", "Alta deportiva");
        Tratamiento t2 = tratamiento(fabrica, 2, l2, fernandez, "2026-01-11", "Fisioterapia y readaptación", "Alta deportiva");
        Tratamiento t3 = tratamiento(fabrica, 3, l3, sosa, "2025-11-03", "Readaptación funcional", "Alta deportiva");
        Tratamiento t4 = tratamiento(fabrica, 4, l4, sosa, "2026-03-06", "Fisioterapia", "Alta deportiva");
        Tratamiento t5 = tratamiento(fabrica, 5, l5, fernandez, "2026-04-18", "Crioterapia y reposo relativo", "Alta deportiva");
        Tratamiento t6 = tratamiento(fabrica, 6, l6, sosa, "2026-05-13", "Readaptación funcional", "Alta deportiva");
        Tratamiento t7 = tratamiento(fabrica, 7, l7, fernandez, "2026-06-16", "Fisioterapia y fortalecimiento excéntrico",
                "Posible recurrencia alertada al registrar la lesión");
        Tratamiento t8 = tratamiento(fabrica, 8, l8, sosa, "2026-07-02", "Readaptación funcional", "Alta deportiva");
        Tratamiento t9 = tratamiento(fabrica, 9, l9, fernandez, "2026-08-03", "Propiocepción y vendaje funcional", null);
        Tratamiento t10 = tratamiento(fabrica, 10, l10, sosa, "2026-08-21", "Fisioterapia", "Alta deportiva");

        evolucion(fabrica, 1, t2, "2026-01-25", "Disminuye el dolor a la palpación", ResultadoEvolucion.FAVORABLE);
        evolucion(fabrica, 2, t2, "2026-02-10", "Tolera carrera continua sin dolor", ResultadoEvolucion.FAVORABLE);
        evolucion(fabrica, 3, t2, "2026-02-20", "Completa pruebas de campo; alta", ResultadoEvolucion.FAVORABLE);
        evolucion(fabrica, 4, t6, "2026-05-27", "Progresa la fuerza de aductores", ResultadoEvolucion.FAVORABLE);
        evolucion(fabrica, 5, t7, "2026-06-30", "Dolor persistente en aceleraciones", ResultadoEvolucion.ESTABLE);
        evolucion(fabrica, 6, t7, "2026-07-20", "Molestia en trabajo excéntrico", ResultadoEvolucion.DESFAVORABLE);
        evolucion(fabrica, 7, t7, "2026-08-25", "Mejora la fuerza excéntrica", ResultadoEvolucion.FAVORABLE);
        evolucion(fabrica, 8, t7, "2026-09-18", "Inicia trabajo con pelota", ResultadoEvolucion.FAVORABLE);
        evolucion(fabrica, 9, t8, "2026-07-20", "Sin dolor en carrera continua", ResultadoEvolucion.FAVORABLE);
        evolucion(fabrica, 10, t9, "2026-08-17", "Disminuye la inflamación", ResultadoEvolucion.FAVORABLE);
        evolucion(fabrica, 11, t9, "2026-09-10", "Estabilidad articular en progreso", ResultadoEvolucion.ESTABLE);

        // Tratamientos 7 y 9 quedan en curso.
        TratamientoDAOMemoria tratamientos = fabrica.getTratamientoDAO();
        finalizar(tratamientos, t1, "2025-11-05");
        finalizar(tratamientos, t2, "2026-02-20");
        finalizar(tratamientos, t3, "2025-12-10");
        finalizar(tratamientos, t4, "2026-03-30");
        finalizar(tratamientos, t5, "2026-04-28");
        finalizar(tratamientos, t6, "2026-06-10");
        finalizar(tratamientos, t8, "2026-08-05");
        finalizar(tratamientos, t10, "2026-09-04");
    }

    private static Jugador jugador(FabricaDAOMemoria fabrica, int id, String dni, String nombre, String apellido,
                                   String nacimiento, int camiseta, String posicion, Categoria categoria) {
        Jugador jugador = new Jugador(id, dni, nombre, apellido, LocalDate.parse(nacimiento), camiseta,
                posicion, true, categoria);
        fabrica.getJugadorDAO().insertar(jugador);
        return jugador;
    }

    private static Lesion lesion(FabricaDAOMemoria fabrica, int id, Jugador jugador, TipoLesion tipo,
                                 ZonaCorporal zona, Gravedad gravedad, String fecha, String descripcion) {
        Lesion lesion = new Lesion(id, jugador, tipo, zona, gravedad, LocalDate.parse(fecha), descripcion);
        fabrica.getLesionDAO().insertar(lesion);
        return lesion;
    }

    private static Tratamiento tratamiento(FabricaDAOMemoria fabrica, int id, Lesion lesion, Kinesiologo kinesiologo,
                                           String inicio, String tipo, String observaciones) {
        Tratamiento tratamiento = new Tratamiento(id, lesion, kinesiologo, LocalDate.parse(inicio), tipo,
                observaciones);
        lesion.agregarTratamiento(tratamiento);
        fabrica.getTratamientoDAO().insertar(tratamiento);
        return tratamiento;
    }

    private static void evolucion(FabricaDAOMemoria fabrica, int id, Tratamiento tratamiento, String fecha,
                                  String observacion, ResultadoEvolucion resultado) {
        Evolucion evolucion = new Evolucion(id, LocalDate.parse(fecha), observacion, resultado);
        tratamiento.agregarEvolucion(evolucion);
        fabrica.getEvolucionDAO().insertar(tratamiento.getId(), evolucion);
    }

    private static void finalizar(TratamientoDAOMemoria tratamientos, Tratamiento tratamiento, String fechaFin) {
        LocalDate fin = LocalDate.parse(fechaFin);
        tratamiento.finalizar(fin);
        tratamientos.finalizar(tratamiento.getId(), fin);
    }
}
