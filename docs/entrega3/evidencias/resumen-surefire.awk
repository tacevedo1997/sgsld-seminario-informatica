# Resume un informe XML de Surefire (TEST-*.xml).
#   awk -v modo=cabecera -f resumen-surefire.awk TEST-x.xml   -> "Clase (N pruebas, F fallas)"
#   awk -v modo=casos    -f resumen-surefire.awk TEST-x.xml   -> una línea "[OK]" o "[FALLA]" por prueba
# Los nombres son los @DisplayName (códigos del plan de pruebas).

function atributo(linea, nombre,    patron, valor) {
    patron = nombre "=\"[^\"]*\""
    if (!match(linea, patron)) {
        return ""
    }
    valor = substr(linea, RSTART + length(nombre) + 2, RLENGTH - length(nombre) - 3)
    gsub(/&quot;/, "\"", valor)
    gsub(/&apos;/, "'", valor)
    gsub(/&lt;/, "<", valor)
    gsub(/&gt;/, ">", valor)
    gsub(/&amp;/, "\\&", valor)
    return valor
}

function cerrarCaso() {
    if (abierto) {
        print (fallo ? "  [FALLA] " : "  [OK]    ") nombre
        abierto = 0
    }
}

/<testsuite / {
    suite = atributo($0, "name")
    total = atributo($0, "tests") + 0
    fallas = atributo($0, "failures") + atributo($0, "errors")
    if (modo == "cabecera" && total > 0) {
        printf "%s (%d %s, %d %s)\n", suite, total, (total == 1 ? "prueba" : "pruebas"), fallas, (fallas == 1 ? "falla" : "fallas")
    }
}

modo == "casos" && /<testcase / {
    cerrarCaso()
    nombre = atributo($0, "name")
    abierto = 1
    fallo = 0
    if ($0 ~ /\/>[[:space:]]*$/) {
        cerrarCaso()
    }
}

modo == "casos" && /<(failure|error)[ >]/ {
    fallo = 1
}

modo == "casos" && /<\/testcase>/ {
    cerrarCaso()
}

END {
    if (modo == "casos") {
        cerrarCaso()
    }
}
