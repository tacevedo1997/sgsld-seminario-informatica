# Evidencias de ejecución · Entrega 3

Transcripciones **reales** de la aplicación de consola de la entrega 3 (etiqueta `entrega-3`), generadas con la entrada redirigida desde los archivos de `entradas/`, el eco activado y la fecha fijada en la del plan de pruebas:

```bash
java -Dsgsld.eco=true -Dsgsld.hoy=2026-09-26 -jar target/sgsld.jar < entradas/<escenario>.txt
```

Desde la versión final (entrega 4), el JAR abre la interfaz JavaFX y usa MySQL por defecto. La misma consola, con el lote de prueba en memoria y sin servidor, se ejecuta con:

```bash
java -Dsgsld.datos=memoria -Dsgsld.eco=true -Dsgsld.hoy=2026-09-26 -jar target/sgsld.jar --consola < entradas/<escenario>.txt
```

Para regenerarlas junto con `resultados-pruebas.txt`, desde Git Bash en la raíz del repositorio (el script ya usa el comando anterior):

```bash
bash docs/entrega3/evidencias/generar-evidencias.sh
```

Con la versión final, las transcripciones regeneradas solo difieren de estas en la primera línea de la pantalla inicial ("Modo demostración: datos de prueba en memoria…"), y `resultados-pruebas.txt` incluye las pruebas agregadas en la entrega 4.

En Windows, Java escribe la salida redirigida en `windows-1252`; el script la convierte a UTF-8 con `iconv`, sin otro cambio. Como no hay una terminal interactiva, la aplicación avisa que la contraseña será visible y el eco la muestra como `********`. Los archivos de entrada están en ASCII; las búsquedas no distinguen tildes, por eso "suarez" encuentra a "Suarez" y "gomez" a "Gómez".

| Archivo | Escenario | Qué muestra |
|---|---|---|
| `01-login-y-excepciones.txt` | Coordinador | Credenciales inválidas (usuario existente con clave incorrecta y usuario inexistente, mismo mensaje) y reintento; letra en el menú (`InputMismatchException`); opción fuera de rango; fechas con formato inválido (`2026-01-01`) e inexistente (`31/02/2026`); búsqueda por DNI sin resultados (`EntidadNoEncontradaException`); alta de categoría con dos errores informados juntos (`ValidacionException`) |
| `02-ciclo-completo-CPA01.txt` | CPA01 en una ejecución | El coordinador da de alta a Martín Suárez; la kinesióloga ve su historial vacío, registra la lesión y el tratamiento; el cuerpo técnico lo ve "En tratamiento"; la kinesióloga registra una evolución y finaliza (5 días); el cuerpo técnico lo ve "Disponible"; el coordinador encuentra la lesión en la ficha y en los reportes (Cuádriceps 2, total 9; Finalizado 9) |
| `03-alerta-recurrencia.txt` | Kinesióloga | Juan Pérez, tobillo, 26/09/2026: sin alerta. Pedro Gómez, isquiotibiales, 26/09/2026: alerta por la lesión 7 (tratamiento en curso); la lesión 2 no alerta porque su tratamiento terminó el 20/02/2026, antes del límite del 26/03/2026. Además se reproduce el texto de la Figura 11 (Pedro Gómez, 15/06/2026: antecedente lesión 2, "3 meses y 26 días"). Intento de un segundo tratamiento en curso para la lesión 7: rechazo |
| `04-reportes-y-ficha.txt` | Coordinador | Ficha de Pedro Gómez con el detalle de la lesión 7; reportes por zona (total 8) y de duración promedio (Muscular 31,5; Contusión 12,0) para el período por defecto 01/01/2026 a 26/09/2026 |
| `resultados-pruebas.txt` | `mvn -B clean package` | Extracto real de la salida de Maven y detalle de las 97 pruebas según los informes de Surefire |

Cada ejecución parte del lote de prueba: los cambios de un archivo no afectan a los demás.
