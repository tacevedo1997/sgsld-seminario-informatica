-- =====================================================================
-- SGSLD · Sistema de Gestión y Seguimiento de Lesiones Deportivas
-- Script de base de datos · MySQL 8.0 · TP2
-- Seminario de Práctica de Informática · Acevedo Ferreyra, Tomás
-- ---------------------------------------------------------------------
-- Ejecución sugerida: MySQL Workbench (Query > Execute All) o
--   mysql -u root -p < sgsld_script.sql
-- Contraseña de todos los usuarios de prueba: Sgsld2026!
-- Las pruebas que deben fallar (sección 5) están comentadas: ejecutarlas de a una para observar el error esperado.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. CREACIÓN DE LA BASE DE DATOS
-- ---------------------------------------------------------------------
DROP DATABASE IF EXISTS sgsld;
CREATE DATABASE sgsld CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE sgsld;

-- ---------------------------------------------------------------------
-- 2. CREACIÓN DE TABLAS
-- ---------------------------------------------------------------------
CREATE TABLE categoria (
    id_categoria  INT              NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(40)      NOT NULL,
    edad_min      TINYINT UNSIGNED NOT NULL,
    edad_max      TINYINT UNSIGNED NOT NULL,
    CONSTRAINT pk_categoria PRIMARY KEY (id_categoria),
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre),
    CONSTRAINT ck_categoria_edades CHECK (edad_min <= edad_max)
) ENGINE = InnoDB;

CREATE TABLE usuario (
    id_usuario       INT          NOT NULL AUTO_INCREMENT,
    nombre_usuario   VARCHAR(30)  NOT NULL,
    hash_contrasena  VARCHAR(120) NOT NULL,
    rol              ENUM('COORDINADOR', 'KINESIOLOGO', 'CUERPO_TECNICO') NOT NULL,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_nombre UNIQUE (nombre_usuario)
) ENGINE = InnoDB;

CREATE TABLE kinesiologo (
    id_kinesiologo  INT         NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(50) NOT NULL,
    apellido        VARCHAR(50) NOT NULL,
    matricula       VARCHAR(20) NOT NULL,
    id_usuario      INT         NULL,
    CONSTRAINT pk_kinesiologo PRIMARY KEY (id_kinesiologo),
    CONSTRAINT uq_kinesiologo_matricula UNIQUE (matricula),
    CONSTRAINT uq_kinesiologo_usuario UNIQUE (id_usuario),
    CONSTRAINT fk_kinesiologo_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE jugador (
    id_jugador        INT              NOT NULL AUTO_INCREMENT,
    dni               VARCHAR(10)      NOT NULL,
    nombre            VARCHAR(50)      NOT NULL,
    apellido          VARCHAR(50)      NOT NULL,
    fecha_nacimiento  DATE             NOT NULL,
    numero_camiseta   TINYINT UNSIGNED NULL,
    posicion          VARCHAR(30)      NOT NULL,
    activo            BOOLEAN          NOT NULL DEFAULT TRUE,
    id_categoria      INT              NOT NULL,
    CONSTRAINT pk_jugador PRIMARY KEY (id_jugador),
    CONSTRAINT uq_jugador_dni UNIQUE (dni),
    CONSTRAINT ck_jugador_camiseta CHECK (numero_camiseta BETWEEN 1 AND 99),
    CONSTRAINT fk_jugador_categoria FOREIGN KEY (id_categoria)
        REFERENCES categoria (id_categoria) ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE zona_corporal (
    id_zona  INT         NOT NULL AUTO_INCREMENT,
    nombre   VARCHAR(50) NOT NULL,
    region   ENUM('Miembro inferior', 'Miembro superior', 'Tronco', 'Cabeza y cuello') NOT NULL,
    CONSTRAINT pk_zona_corporal PRIMARY KEY (id_zona),
    CONSTRAINT uq_zona_corporal_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE tipo_lesion (
    id_tipo  INT         NOT NULL AUTO_INCREMENT,
    nombre   VARCHAR(50) NOT NULL,
    CONSTRAINT pk_tipo_lesion PRIMARY KEY (id_tipo),
    CONSTRAINT uq_tipo_lesion_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE lesion (
    id_lesion    INT          NOT NULL AUTO_INCREMENT,
    id_jugador   INT          NOT NULL,
    id_tipo      INT          NOT NULL,
    id_zona      INT          NOT NULL,
    gravedad     ENUM('Leve', 'Moderada', 'Grave') NOT NULL,
    fecha        DATE         NOT NULL,
    descripcion  VARCHAR(500) NULL,
    CONSTRAINT pk_lesion PRIMARY KEY (id_lesion),
    CONSTRAINT fk_lesion_jugador FOREIGN KEY (id_jugador)
        REFERENCES jugador (id_jugador) ON DELETE RESTRICT,
    CONSTRAINT fk_lesion_tipo FOREIGN KEY (id_tipo)
        REFERENCES tipo_lesion (id_tipo) ON DELETE RESTRICT,
    CONSTRAINT fk_lesion_zona FOREIGN KEY (id_zona)
        REFERENCES zona_corporal (id_zona) ON DELETE RESTRICT,
    INDEX idx_lesion_jugador_zona_fecha (id_jugador, id_zona, fecha),
    INDEX idx_lesion_fecha (fecha)
) ENGINE = InnoDB;

CREATE TABLE tratamiento (
    id_tratamiento   INT          NOT NULL AUTO_INCREMENT,
    id_lesion        INT          NOT NULL,
    id_kinesiologo   INT          NOT NULL,
    fecha_inicio     DATE         NOT NULL,
    fecha_fin        DATE         NULL,
    tipo             VARCHAR(60)  NOT NULL,
    estado           ENUM('En curso', 'Finalizado') NOT NULL DEFAULT 'En curso',
    observaciones    VARCHAR(500) NULL,
    -- Vale id_lesion solo mientras el tratamiento está en curso; el índice
    -- único impide un segundo tratamiento en curso para la misma lesión.
    lesion_en_curso  INT GENERATED ALWAYS AS (IF(estado = 'En curso', id_lesion, NULL)) STORED,
    CONSTRAINT pk_tratamiento PRIMARY KEY (id_tratamiento),
    CONSTRAINT uq_tratamiento_en_curso UNIQUE (lesion_en_curso),
    CONSTRAINT ck_tratamiento_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT ck_tratamiento_estado CHECK (
        (estado = 'En curso' AND fecha_fin IS NULL) OR
        (estado = 'Finalizado' AND fecha_fin IS NOT NULL)),
    CONSTRAINT fk_tratamiento_lesion FOREIGN KEY (id_lesion)
        REFERENCES lesion (id_lesion) ON DELETE RESTRICT,
    CONSTRAINT fk_tratamiento_kinesiologo FOREIGN KEY (id_kinesiologo)
        REFERENCES kinesiologo (id_kinesiologo) ON DELETE RESTRICT,
    INDEX idx_tratamiento_estado_fin (estado, fecha_fin)
) ENGINE = InnoDB;

CREATE TABLE evolucion (
    id_evolucion    INT          NOT NULL AUTO_INCREMENT,
    id_tratamiento  INT          NOT NULL,
    fecha           DATE         NOT NULL,
    observacion     VARCHAR(500) NOT NULL,
    resultado       ENUM('Favorable', 'Estable', 'Desfavorable') NOT NULL,
    CONSTRAINT pk_evolucion PRIMARY KEY (id_evolucion),
    CONSTRAINT fk_evolucion_tratamiento FOREIGN KEY (id_tratamiento)
        REFERENCES tratamiento (id_tratamiento) ON DELETE RESTRICT,
    INDEX idx_evolucion_tratamiento_fecha (id_tratamiento, fecha)
) ENGINE = InnoDB;

-- Vista para el Cuerpo Técnico (CU-08): el estado se deriva, no se almacena.
CREATE VIEW v_estado_jugador AS
SELECT j.id_jugador,
       CONCAT(j.apellido, ', ', j.nombre) AS jugador,
       c.nombre AS categoria,
       CASE WHEN EXISTS (SELECT 1
                         FROM lesion l
                         JOIN tratamiento t ON t.id_lesion = l.id_lesion
                         WHERE l.id_jugador = j.id_jugador
                           AND t.estado = 'En curso')
            THEN 'En tratamiento'
            ELSE 'Disponible'
       END AS estado
FROM jugador j
JOIN categoria c ON c.id_categoria = j.id_categoria
WHERE j.activo = TRUE;

-- ---------------------------------------------------------------------
-- 3. INSERCIÓN DE DATOS DE PRUEBA
-- ---------------------------------------------------------------------
INSERT INTO categoria (id_categoria, nombre, edad_min, edad_max) VALUES
    (1, 'Primera', 18, 40),
    (2, 'Reserva', 18, 23),
    (3, 'Sub-17',  16, 17),
    (4, 'Sub-15',  14, 15);

INSERT INTO usuario (id_usuario, nombre_usuario, hash_contrasena, rol) VALUES
    (1, 'mlopez',     'pbkdf2_sha256$600000$9tMaGR8tPF6YEDLEVayIyw==$LZeYsYJMJVj/3d6krUtBIQSx6tWji/r21RIlOAEdnJc=', 'COORDINADOR'),
    (2, 'lfernandez', 'pbkdf2_sha256$600000$26N3kgXnzRtg4Rwns5UpCg==$FSrI8YsD/HSvjMnXMkMJB4OT9D2educQw/LVTVkHqJw=', 'KINESIOLOGO'),
    (3, 'asosa',      'pbkdf2_sha256$600000$zzhls+mk8j3ECd3FpaymwA==$r2jHjIAQ4SLnyPBe9r1FbFnVvbo4R05+z0cPHJZ4VNY=', 'KINESIOLOGO'),
    (4, 'ctecnico',   'pbkdf2_sha256$600000$nAQW/lf9vCWoJjBOB3UjQA==$y4j/gFkevLA1cR2HKsCKuAAycral/gvBPuzwDVANuYs=', 'CUERPO_TECNICO');

INSERT INTO kinesiologo (id_kinesiologo, nombre, apellido, matricula, id_usuario) VALUES
    (1, 'Laura',  'Fernández', 'MP-10234', 2),
    (2, 'Andrés', 'Sosa',      'MP-11872', 3),
    (3, 'Carlos', 'Ruiz',      'MP-12950', NULL);

INSERT INTO zona_corporal (id_zona, nombre, region) VALUES
    (1, 'Isquiotibiales', 'Miembro inferior'),
    (2, 'Cuádriceps',     'Miembro inferior'),
    (3, 'Tobillo',        'Miembro inferior'),
    (4, 'Rodilla',        'Miembro inferior'),
    (5, 'Aductores',      'Miembro inferior'),
    (6, 'Hombro',         'Miembro superior'),
    (7, 'Zona lumbar',    'Tronco');

INSERT INTO tipo_lesion (id_tipo, nombre) VALUES
    (1, 'Muscular'), (2, 'Ligamentaria'), (3, 'Tendinosa'), (4, 'Ósea'), (5, 'Contusión');

INSERT INTO jugador (id_jugador, dni, nombre, apellido, fecha_nacimiento, numero_camiseta, posicion, id_categoria) VALUES
    (1, '41236587', 'Juan',    'Pérez',   '1998-03-14',  9, 'Delantero',     1),
    (2, '42587634', 'Pedro',   'Gómez',   '2000-07-22',  5, 'Mediocampista', 1),
    (3, '45120398', 'Luis',    'Díaz',    '2004-02-10',  4, 'Defensor',      2),
    (4, '49365120', 'Matías',  'Romero',  '2009-04-18', 10, 'Mediocampista', 3),
    (5, '50214786', 'Tomás',   'Herrera', '2010-01-25',  3, 'Defensor',      3),
    (6, '46078912', 'Franco',  'Acosta',  '2005-06-30', 11, 'Delantero',     2),
    (7, '51347025', 'Nicolás', 'Castro',  '2011-09-02',  7, 'Delantero',     4),
    (8, '39874521', 'Lucas',   'Molina',  '1996-11-05',  1, 'Arquero',       1);

INSERT INTO lesion (id_lesion, id_jugador, id_tipo, id_zona, gravedad, fecha, descripcion) VALUES
    ( 1, 1, 2, 3, 'Moderada', '2025-09-20', 'Esguince de tobillo derecho en entrenamiento'),
    ( 2, 2, 1, 1, 'Moderada', '2026-01-10', 'Distensión en isquiotibiales durante un sprint'),
    ( 3, 6, 1, 1, 'Leve',     '2025-11-02', 'Molestia en isquiotibiales tras el partido'),
    ( 4, 4, 1, 2, 'Leve',     '2026-03-05', 'Sobrecarga en cuádriceps'),
    ( 5, 3, 5, 4, 'Leve',     '2026-04-18', 'Golpe en la rodilla en un choque'),
    ( 6, 5, 1, 5, 'Moderada', '2026-05-12', 'Distensión de aductores'),
    ( 7, 2, 1, 1, 'Grave',    '2026-06-15', 'Dolor agudo en sprint durante el partido'),
    ( 8, 6, 1, 1, 'Moderada', '2026-07-01', 'Distensión en isquiotibiales en entrenamiento'),
    ( 9, 5, 2, 3, 'Moderada', '2026-08-02', 'Esguince de tobillo izquierdo'),
    (10, 7, 5, 6, 'Leve',     '2026-08-20', 'Golpe en el hombro por caída');

INSERT INTO tratamiento (id_tratamiento, id_lesion, id_kinesiologo, fecha_inicio, fecha_fin, tipo, estado, observaciones) VALUES
    ( 1,  1, 1, '2025-09-21', '2025-11-05', 'Fisioterapia y propiocepción',              'Finalizado', 'Alta deportiva'),
    ( 2,  2, 1, '2026-01-11', '2026-02-20', 'Fisioterapia y readaptación',               'Finalizado', 'Alta deportiva'),
    ( 3,  3, 2, '2025-11-03', '2025-12-10', 'Readaptación funcional',                    'Finalizado', 'Alta deportiva'),
    ( 4,  4, 2, '2026-03-06', '2026-03-30', 'Fisioterapia',                              'Finalizado', 'Alta deportiva'),
    ( 5,  5, 1, '2026-04-18', '2026-04-28', 'Crioterapia y reposo relativo',             'Finalizado', 'Alta deportiva'),
    ( 6,  6, 2, '2026-05-13', '2026-06-10', 'Readaptación funcional',                    'Finalizado', 'Alta deportiva'),
    ( 7,  7, 1, '2026-06-16', NULL,         'Fisioterapia y fortalecimiento excéntrico', 'En curso',   'Posible recurrencia alertada al registrar la lesión'),
    ( 8,  8, 2, '2026-07-02', '2026-08-05', 'Readaptación funcional',                    'Finalizado', 'Alta deportiva'),
    ( 9,  9, 1, '2026-08-03', NULL,         'Propiocepción y vendaje funcional',         'En curso',   NULL),
    (10, 10, 2, '2026-08-21', '2026-09-04', 'Fisioterapia',                              'Finalizado', 'Alta deportiva');

INSERT INTO evolucion (id_evolucion, id_tratamiento, fecha, observacion, resultado) VALUES
    ( 1, 2, '2026-01-25', 'Disminuye el dolor a la palpación',  'Favorable'),
    ( 2, 2, '2026-02-10', 'Tolera carrera continua sin dolor',  'Favorable'),
    ( 3, 2, '2026-02-20', 'Completa pruebas de campo; alta',    'Favorable'),
    ( 4, 6, '2026-05-27', 'Progresa la fuerza de aductores',    'Favorable'),
    ( 5, 7, '2026-06-30', 'Dolor persistente en aceleraciones', 'Estable'),
    ( 6, 7, '2026-07-20', 'Molestia en trabajo excéntrico',     'Desfavorable'),
    ( 7, 7, '2026-08-25', 'Mejora la fuerza excéntrica',        'Favorable'),
    ( 8, 7, '2026-09-18', 'Inicia trabajo con pelota',          'Favorable'),
    ( 9, 8, '2026-07-20', 'Sin dolor en carrera continua',      'Favorable'),
    (10, 9, '2026-08-17', 'Disminuye la inflamación',           'Favorable'),
    (11, 9, '2026-09-10', 'Estabilidad articular en progreso',  'Estable');

-- Verificación de la carga
SELECT 'categoria' AS tabla, COUNT(*) AS registros FROM categoria
UNION ALL SELECT 'usuario',       COUNT(*) FROM usuario
UNION ALL SELECT 'kinesiologo',   COUNT(*) FROM kinesiologo
UNION ALL SELECT 'zona_corporal', COUNT(*) FROM zona_corporal
UNION ALL SELECT 'tipo_lesion',   COUNT(*) FROM tipo_lesion
UNION ALL SELECT 'jugador',       COUNT(*) FROM jugador
UNION ALL SELECT 'lesion',        COUNT(*) FROM lesion
UNION ALL SELECT 'tratamiento',   COUNT(*) FROM tratamiento
UNION ALL SELECT 'evolucion',     COUNT(*) FROM evolucion;

-- ---------------------------------------------------------------------
-- 4. CONSULTAS
-- ---------------------------------------------------------------------
-- C1. Historial de un jugador (CU-12, CU-10): Pedro Gómez
SELECT l.fecha   AS fecha_lesion,
       z.nombre  AS zona,
       tl.nombre AS tipo,
       l.gravedad,
       t.tipo    AS tratamiento,
       t.estado,
       t.fecha_fin,
       e.fecha   AS fecha_evolucion,
       e.resultado
FROM lesion l
JOIN zona_corporal z ON z.id_zona = l.id_zona
JOIN tipo_lesion tl  ON tl.id_tipo = l.id_tipo
LEFT JOIN tratamiento t ON t.id_lesion = l.id_lesion
LEFT JOIN evolucion e   ON e.id_tratamiento = t.id_tratamiento
WHERE l.id_jugador = 2
ORDER BY l.fecha, t.fecha_inicio, e.fecha;

-- C2. Verificación de recurrencia (CU-11) antes de registrar una lesión
--     Caso A: Pedro Gómez, isquiotibiales, 15/06/2026 -> devuelve el antecedente
SET @id_jugador = 2, @id_zona = 1, @fecha_nueva = '2026-06-15';
SELECT l.id_lesion,
       l.fecha  AS fecha_antecedente,
       z.nombre AS zona,
       t.estado,
       t.fecha_fin,
       DATE_SUB(@fecha_nueva, INTERVAL 6 MONTH) AS limite_ventana
FROM lesion l
JOIN zona_corporal z ON z.id_zona = l.id_zona
LEFT JOIN tratamiento t ON t.id_lesion = l.id_lesion
WHERE l.id_jugador = @id_jugador
  AND l.id_zona    = @id_zona
  AND l.fecha      < @fecha_nueva
  AND (t.id_tratamiento IS NULL
       OR t.estado = 'En curso'
       OR t.fecha_fin >= DATE_SUB(@fecha_nueva, INTERVAL 6 MONTH));

--     Caso B: Franco Acosta, isquiotibiales, 01/07/2026 -> sin resultados
SET @id_jugador = 6, @id_zona = 1, @fecha_nueva = '2026-07-01';
SELECT l.id_lesion,
       l.fecha  AS fecha_antecedente,
       z.nombre AS zona,
       t.estado,
       t.fecha_fin,
       DATE_SUB(@fecha_nueva, INTERVAL 6 MONTH) AS limite_ventana
FROM lesion l
JOIN zona_corporal z ON z.id_zona = l.id_zona
LEFT JOIN tratamiento t ON t.id_lesion = l.id_lesion
WHERE l.id_jugador = @id_jugador
  AND l.id_zona    = @id_zona
  AND l.fecha      < @fecha_nueva
  AND (t.id_tratamiento IS NULL
       OR t.estado = 'En curso'
       OR t.fecha_fin >= DATE_SUB(@fecha_nueva, INTERVAL 6 MONTH));

-- C3. Estado actual de disponibilidad (CU-08)
SELECT jugador, categoria, estado
FROM v_estado_jugador
ORDER BY categoria, jugador;

-- Período de los reportes (CU-07)
SET @desde = '2026-01-01', @hasta = '2026-09-26';

-- C4. Lesiones por categoría
SELECT c.nombre AS categoria, COUNT(*) AS lesiones
FROM lesion l
JOIN jugador j   ON j.id_jugador = l.id_jugador
JOIN categoria c ON c.id_categoria = j.id_categoria
WHERE l.fecha BETWEEN @desde AND @hasta
GROUP BY c.nombre
ORDER BY lesiones DESC, categoria;

-- C5. Lesiones por zona corporal
SELECT z.nombre AS zona_corporal, COUNT(*) AS lesiones
FROM lesion l
JOIN zona_corporal z ON z.id_zona = l.id_zona
WHERE l.fecha BETWEEN @desde AND @hasta
GROUP BY z.nombre
ORDER BY lesiones DESC, zona_corporal;

-- C6. Lesiones por tipo
SELECT tl.nombre AS tipo_lesion, COUNT(*) AS lesiones
FROM lesion l
JOIN tipo_lesion tl ON tl.id_tipo = l.id_tipo
WHERE l.fecha BETWEEN @desde AND @hasta
GROUP BY tl.nombre
ORDER BY lesiones DESC, tipo_lesion;

-- C7. Lesiones por período (mes)
SELECT DATE_FORMAT(l.fecha, '%Y-%m') AS mes, COUNT(*) AS lesiones
FROM lesion l
WHERE l.fecha BETWEEN @desde AND @hasta
GROUP BY mes
ORDER BY mes;

-- C8. Duración promedio de tratamientos finalizados por tipo de lesión (RF11)
SELECT tl.nombre AS tipo_lesion,
       COUNT(*)  AS tratamientos_finalizados,
       ROUND(AVG(DATEDIFF(t.fecha_fin, t.fecha_inicio)), 1) AS duracion_promedio_dias
FROM tratamiento t
JOIN lesion l       ON l.id_lesion = t.id_lesion
JOIN tipo_lesion tl ON tl.id_tipo = l.id_tipo
WHERE t.estado = 'Finalizado'
  AND t.fecha_fin BETWEEN @desde AND @hasta
GROUP BY tl.nombre
ORDER BY duracion_promedio_dias DESC;

-- C9. Tratamientos activos y finalizados
SELECT estado, COUNT(*) AS cantidad
FROM tratamiento
GROUP BY estado
ORDER BY estado;

-- C10. Pregunta planteada en el AP1: lesiones musculares en Sub-17 durante 2026
SELECT COUNT(*) AS lesiones_musculares_sub17_2026
FROM lesion l
JOIN jugador j      ON j.id_jugador = l.id_jugador
JOIN categoria c    ON c.id_categoria = j.id_categoria
JOIN tipo_lesion tl ON tl.id_tipo = l.id_tipo
WHERE c.nombre  = 'Sub-17'
  AND tl.nombre = 'Muscular'
  AND l.fecha BETWEEN '2026-01-01' AND '2026-12-31';

-- ---------------------------------------------------------------------
-- 5. PRUEBAS DE INTEGRIDAD (deben fallar; ejecutar de a una)
-- ---------------------------------------------------------------------
-- P1. Segundo tratamiento en curso para la lesión 7 (CU-05, 4a)
-- INSERT INTO tratamiento (id_lesion, id_kinesiologo, fecha_inicio, tipo)
-- VALUES (7, 2, '2026-09-20', 'Readaptación funcional');
--   -> ERROR 1062: Duplicate entry '7' for key 'tratamiento.uq_tratamiento_en_curso'
-- P2. DNI duplicado (CU-01, 4a)
-- INSERT INTO jugador (dni, nombre, apellido, fecha_nacimiento, posicion, id_categoria)
-- VALUES ('42587634', 'Martín', 'Suárez', '2001-05-10', 'Defensor', 1);
--   -> ERROR 1062: Duplicate entry '42587634' for key 'jugador.uq_jugador_dni'
-- P3. Fecha de fin anterior a la de inicio (CU-06)
-- UPDATE tratamiento SET estado = 'Finalizado', fecha_fin = '2026-06-01'
-- WHERE id_tratamiento = 7;
--   -> ERROR 3819: Check constraint 'ck_tratamiento_fechas' is violated.
-- P4. Borrado físico de un jugador con historial (CU-01, 3b)
-- DELETE FROM jugador WHERE id_jugador = 2;
--   -> ERROR 1451: Cannot delete or update a parent row: a foreign key constraint fails (... fk_lesion_jugador ...)

-- ---------------------------------------------------------------------
-- 6. MODIFICACIÓN Y BORRADO DE REGISTROS
-- ---------------------------------------------------------------------
-- B1. Baja lógica de un jugador (CU-01, 3b): deja de figurar en la vista
UPDATE jugador SET activo = FALSE WHERE dni = '39874521';
SELECT jugador, categoria, estado FROM v_estado_jugador ORDER BY categoria, jugador;

-- B2. Borrado de un registro puntual: evolución cargada por error
DELETE FROM evolucion WHERE id_evolucion = 11;
SELECT id_evolucion, fecha, observacion FROM evolucion WHERE id_tratamiento = 9;

-- ---------------------------------------------------------------------
-- 7. LIMPIEZA DEL LOTE DE PRUEBA (omitir para conservar los datos)
-- ---------------------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;   -- MySQL Workbench bloquea DELETE sin clave por defecto
DELETE FROM evolucion;
DELETE FROM tratamiento;
DELETE FROM lesion;
DELETE FROM jugador;
DELETE FROM kinesiologo;
DELETE FROM usuario;
DELETE FROM zona_corporal;
DELETE FROM tipo_lesion;
DELETE FROM categoria;
SET SQL_SAFE_UPDATES = 1;

SELECT 'categoria' AS tabla, COUNT(*) AS registros FROM categoria
UNION ALL SELECT 'usuario',       COUNT(*) FROM usuario
UNION ALL SELECT 'kinesiologo',   COUNT(*) FROM kinesiologo
UNION ALL SELECT 'zona_corporal', COUNT(*) FROM zona_corporal
UNION ALL SELECT 'tipo_lesion',   COUNT(*) FROM tipo_lesion
UNION ALL SELECT 'jugador',       COUNT(*) FROM jugador
UNION ALL SELECT 'lesion',        COUNT(*) FROM lesion
UNION ALL SELECT 'tratamiento',   COUNT(*) FROM tratamiento
UNION ALL SELECT 'evolucion',     COUNT(*) FROM evolucion;

/*
-- ---------------------------------------------------------------------
-- 8. USUARIO DE LA APLICACIÓN (opcional; requiere privilegios de administrador)
-- ---------------------------------------------------------------------
-- CREATE USER 'sgsld_app'@'192.168.10.%' IDENTIFIED BY 'cambiar-esta-clave' REQUIRE SSL;
-- GRANT SELECT, INSERT, UPDATE ON sgsld.* TO 'sgsld_app'@'192.168.10.%';
-- Sin permiso DELETE: la aplicación solo realiza bajas lógicas.
*/


