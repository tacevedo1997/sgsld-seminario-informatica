-- =====================================================================
-- SGSLD · Usuario de las pruebas automatizadas con MySQL
-- ---------------------------------------------------------------------
-- Las pruebas con la etiqueta "mysql" (mvn test -Dgroups=mysql) usan una
-- base aparte, sgsld_test, que eliminan y vuelven a crear desde
-- sgsld_script.sql antes de cada clase de prueba. Este usuario solo tiene
-- privilegios sobre esa base: nunca puede tocar la base sgsld.
--
-- Ejecución: MySQL Workbench con un usuario administrador (por ejemplo,
-- root). No hace falta crear sgsld_test: la crean las pruebas.
-- 1. Reemplace CAMBIAR_CLAVE por una clave elegida por usted, solo en el
--    editor de Workbench: no guarde la clave en este archivo, que está en
--    el repositorio.
-- 2. Ejecute el script (Query > Execute All).
-- 3. Escriba la misma clave en db-test.properties (db.clave).
-- =====================================================================

DROP USER IF EXISTS 'sgsld_pruebas'@'localhost';
CREATE USER 'sgsld_pruebas'@'localhost' IDENTIFIED BY 'CAMBIAR_CLAVE' REQUIRE SSL;
GRANT ALL PRIVILEGES ON sgsld_test.* TO 'sgsld_pruebas'@'localhost';
