#!/usr/bin/env bash
# Regenera las evidencias de la entrega 3 a partir del código actual:
#   1. mvn -B clean package (compila, ejecuta las pruebas y genera target/sgsld.jar)
#      y resume el resultado en resultados-pruebas.txt.
#   2. Ejecuta la consola con cada archivo de entradas/ y guarda la transcripción:
#      java -Dsgsld.datos=memoria -Dsgsld.eco=true -Dsgsld.hoy=2026-09-26 \
#           -jar target/sgsld.jar --consola < entradas/<escenario>.txt
#      Desde la versión final, el JAR abre la interfaz JavaFX y usa MySQL por
#      defecto: --consola elige la vista de consola y -Dsgsld.datos=memoria el
#      lote de prueba en memoria, como en la entrega 3 (no hace falta MySQL).
# Pensado para Git Bash en Windows; también funciona en Linux y macOS.
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/../../.." && pwd)"
DESTINO="$RAIZ/docs/entrega3/evidencias"
cd "$RAIZ"

# En Windows, Java escribe la salida redirigida en windows-1252 (el juego de
# caracteres del sistema); se convierte a UTF-8 sin cambiar el contenido.
case "${OSTYPE:-}" in
    msys* | cygwin* | win32*) a_utf8() { iconv -f CP1252 -t UTF-8; } ;;
    *) a_utf8() { cat; } ;;
esac

salida_maven="$(mktemp)"
trap 'rm -f "$salida_maven"' EXIT

mvn -B clean package 2>&1 | a_utf8 > "$salida_maven"

{
    echo "SGSLD - Resultados de las pruebas automatizadas"
    echo "Comando: mvn -B clean package (compila con -Xlint:all y failOnWarning, ejecuta las pruebas y genera target/sgsld.jar)"
    echo "Fecha de ejecución: $(date '+%d/%m/%Y %H:%M')"
    echo "Entorno: $(java -version 2>&1 | sed -n 1p) | $(mvn -v 2>&1 | sed -n 1p)"
    echo
    echo "1. Salida de Maven (extracto: clases de prueba, totales y tiempos de CPS02)"
    echo
    grep -E '^\[INFO\] (Running|Tests run|BUILD|Building jar)|^\[(WARNING|ERROR)\]|^CPS02' "$salida_maven" || true
    echo
    echo "2. Detalle por prueba (informes de Surefire en target/surefire-reports)"
    for xml in target/surefire-reports/TEST-*.xml; do
        cabecera="$(awk -v modo=cabecera -f "$DESTINO/resumen-surefire.awk" "$xml")"
        if [ -n "$cabecera" ]; then
            echo
            echo "$cabecera"
            awk -v modo=casos -f "$DESTINO/resumen-surefire.awk" "$xml" | LC_ALL=C sort -k2
        fi
    done
} > "$DESTINO/resultados-pruebas.txt"

for entrada in "$DESTINO"/entradas/*.txt; do
    nombre="$(basename "$entrada" .txt)"
    java -Dsgsld.datos=memoria -Dsgsld.eco=true -Dsgsld.hoy=2026-09-26 -jar target/sgsld.jar --consola \
        < "$entrada" | a_utf8 > "$DESTINO/$nombre.txt"
    echo "Generada: docs/entrega3/evidencias/$nombre.txt"
done
echo "Generado: docs/entrega3/evidencias/resultados-pruebas.txt"
