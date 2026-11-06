-- =====================================================================
-- SGSLD · Usuario local de la aplicación
-- ---------------------------------------------------------------------
-- Equivalente local de la sección 15.6 del informe: la aplicación se
-- conecta con un usuario propio, con conexión segura (TLS) obligatoria y
-- sin permiso DELETE, porque solo realiza bajas lógicas.
--
-- Ejecución: MySQL Workbench con un usuario administrador (por ejemplo,
-- root), después de crear la base con sql/crear_base.sql.
-- 1. Reemplace CAMBIAR_CLAVE por una clave elegida por usted, solo en el
--    editor de Workbench: no guarde la clave en este archivo, que está en
--    el repositorio.
-- 2. Ejecute el script (Query > Execute All).
-- 3. Escriba la misma clave en db.properties (db.clave).
-- =====================================================================

DROP USER IF EXISTS 'sgsld_app'@'localhost';
CREATE USER 'sgsld_app'@'localhost' IDENTIFIED BY 'CAMBIAR_CLAVE' REQUIRE SSL;
GRANT SELECT, INSERT, UPDATE ON sgsld.* TO 'sgsld_app'@'localhost';
