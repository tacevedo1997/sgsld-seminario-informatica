# SGSLD · Sistema de Gestión y Seguimiento de Lesiones Deportivas

Proyecto del Seminario de Práctica de Informática (Universidad Siglo 21), desarrollado con el Proceso Unificado de Desarrollo. El SGSLD centraliza el seguimiento de las lesiones de los jugadores de un club con varias categorías: registro de lesiones, tratamientos y evoluciones, alerta de posible lesión recurrente, estado de disponibilidad y reportes estadísticos.

| Rol | Puede |
|---|---|
| Coordinador | Administrar jugadores, categorías y kinesiólogos; consultar fichas y reportes |
| Kinesiólogo | Registrar lesiones, tratamientos y evoluciones; consultar jugadores y fichas |
| Cuerpo Técnico | Consultar solo el estado de disponibilidad, sin información clínica |

## Alcance de este incremento (entrega 3)

- Aplicación de **consola** en Java 17 con menús por rol. La vista de consola reemplaza, en este incremento, a las vistas JavaFX del diseño.
- **Persistencia en memoria** mediante implementaciones `*DAOMemoria` de las interfaces DAO del diseño, cargadas al iniciar con el lote de prueba de `sgsld_script.sql`. **Los cambios no se conservan al salir.** En la entrega final se agregarán las implementaciones MySQL (JDBC) sin modificar modelo ni servicios.
- Cubre los 12 casos de uso (CU-01 a CU-12).
- Sin dependencias en tiempo de ejecución: solo la biblioteca estándar del JDK. JUnit 5 se usa únicamente para las pruebas.

## Requisitos

- **JDK 17 o superior.** El código se compila con `--release 17`, de modo que el compilador rechaza cualquier API o sintaxis posterior a Java 17. Se desarrolló y probó con JDK 23 y Maven 3.9.11 en Windows 11.
- **Maven 3.9** (opcional): sin Maven se puede compilar con `javac`, ver más abajo.

## Compilar y ejecutar con Maven

```bash
mvn clean package
java -jar target/sgsld.jar
```

`mvn clean package` compila con todas las advertencias activadas (`-Xlint:all`) y trata cualquier advertencia como error, ejecuta las pruebas y genera el JAR ejecutable `target/sgsld.jar`.

Propiedades opcionales:

| Propiedad | Efecto |
|---|---|
| `-Dsgsld.hoy=2026-09-26` | Fija la fecha del sistema (formato `aaaa-MM-dd`). La pantalla inicial lo indica. Un valor inválido se informa y se usa el reloj del sistema |
| `-Dsgsld.eco=true` | Repite cada dato leído después de la pregunta, para que las transcripciones con entrada redirigida sean legibles |

Ejemplo con la fecha del plan de pruebas y la entrada redirigida (desde Git Bash o `cmd`):

```bash
java -Dsgsld.eco=true -Dsgsld.hoy=2026-09-26 -jar target/sgsld.jar < docs/entrega3/evidencias/entradas/03-alerta-recurrencia.txt
```

## Compilar y ejecutar sin Maven (`javac`)

No hay dependencias, así que alcanza con el JDK. Desde la raíz del repositorio:

Git Bash, Linux o macOS:

```bash
javac -encoding UTF-8 --release 17 -d out $(find src/main/java -name "*.java")
java -cp out sgsld.App
```

PowerShell:

```powershell
javac -encoding UTF-8 --release 17 -d out (Get-ChildItem -Recurse -Filter *.java src/main/java).FullName
java -cp out sgsld.App
```

Símbolo del sistema (`cmd`):

```bat
mkdir out
dir /s /b src\main\java\*.java > out\fuentes.txt
javac -encoding UTF-8 --release 17 -d out @out\fuentes.txt
java -cp out sgsld.App
```

## Ejecutar desde IntelliJ IDEA

1. *File > Open* y elegir el archivo `pom.xml` de la raíz (abrir como proyecto).
2. En *File > Project Structure > Project*, elegir un SDK 17 o superior.
3. Ejecutar la clase `sgsld.App` (botón de ejecución junto a `main`).
4. Para las pruebas: clic derecho sobre `src/test/java` > *Run 'All Tests'*.

La consola de ejecución del IDE no es una terminal: la contraseña se lee con eco visible y la aplicación lo avisa. Para ocultarla, ejecutar el JAR desde una terminal.

## Usuarios de prueba

La contraseña de todos los usuarios es `Sgsld2026!`.

| Usuario | Rol | Persona |
|---|---|---|
| `mlopez` | Coordinador | — |
| `lfernandez` | Kinesiólogo | Laura Fernández (MP-10234) |
| `asosa` | Kinesiólogo | Andrés Sosa (MP-11872) |
| `ctecnico` | Cuerpo Técnico | — |

Las contraseñas se guardan como hash PBKDF2-HMAC-SHA256 (600.000 iteraciones, sal de 16 bytes). Los hashes son los mismos de `sgsld_script.sql`.

## Pruebas automatizadas

```bash
mvn test
```

Hay 97 pruebas JUnit 5 que usan los códigos del plan de pruebas de la entrega 2 (CPC, CPI, CPS) en sus nombres. Se ejecutan con el lote de prueba y un reloj fijo en el 26/09/2026. La prueba de rendimiento (CPS02) tiene la etiqueta `rendimiento`:

```bash
mvn test -Dgroups=rendimiento
mvn test -DexcludedGroups=rendimiento
```

El primer comando ejecuta solo la prueba de rendimiento; el segundo, todas las demás.

## Consola de Windows

- La aplicación **no fuerza UTF-8**: lee con el juego de caracteres de la consola y escribe con el del sistema, de modo que las tildes se ven bien en `cmd` y PowerShell. Los símbolos de la interfaz son solo ASCII (`+ - |`, `#`, `[OK]`, `[ERROR]`, `[ALERTA]`).
- Con la entrada o la salida redirigidas en Windows, Java usa `windows-1252`. Las evidencias de `docs/entrega3/evidencias` se convirtieron a UTF-8 con `iconv`, sin otro cambio.
- PowerShell 5.1 no admite `<` para redirigir la entrada; usar Git Bash o `cmd /c "java -jar target\sgsld.jar < archivo.txt"`.
- Al agotarse la entrada, la aplicación termina ordenadamente.

## Estructura de paquetes

```
src/main/java/sgsld/
  App.java              raíz de composición: fábrica de DAO, lote de prueba, servicios, controladores y menús
  modelo/               entidades, jerarquía Entidad/Persona, enumeraciones
  modelo/dto/           records de transporte hacia la vista
  dao/                  interfaces DAO y fábrica abstracta FabricaDAO
  dao/memoria/          DAOMemoria<T>, implementaciones en memoria, FabricaDAOMemoria y DatosDePrueba
  servicio/             reglas de negocio y control de acceso por rol
  servicio/reporte/     jerarquía Reporte
  controlador/          flujos de los casos de uso
  vista/                jerarquía Menu
  vista/consola/        Consola, TablaConsola y GraficoConsola (entrada y salida)
  util/                 SesionUsuario, HashContrasena, Algoritmos, Texto, Fechas, Validaciones, excepciones
src/test/java/sgsld/    pruebas JUnit 5 y EscenarioPrueba
docs/entrega3/          evidencias de ejecución
sgsld_script.sql        base de datos MySQL 8.0 de la entrega 2 (no se usa en este incremento)
```

Dependencias entre capas: `vista` → `controlador` → `servicio` → `dao` → `modelo`; todas usan `modelo` y `util`. Ninguna clase de `modelo`, `dao` o `servicio` lee del teclado ni escribe en la consola.

## Requisitos de la consigna y dónde se cumplen

| Requisito | Dónde se cumple (ejemplos) |
|---|---|
| Sintaxis, tipos de datos y estructuras de control | Todo el código; por ejemplo, `LocalDate`, `Integer` opcional y `long` en `modelo/Tratamiento.java`; `switch` con flechas en `vista/MenuPrincipal.java`; `do-while` en `vista/Menu.java` |
| Tratamiento y manejo de excepciones | Jerarquía `util/SgsldException.java`; `InputMismatchException` con `finally` en `vista/consola/Consola.java` (`leerEntero`); `ValidacionException` en los controladores; red de seguridad en `Menu.mostrar()`; fin de entrada en `App.main` |
| Encapsulamiento | Atributos privados; `modelo/Tratamiento.java` sin `setEstado` ni `setFechaFin` (diagrama de estados); listas no modificables en `Lesion` y `Tratamiento` |
| Herencia | `Entidad` → `Persona` → `Jugador`/`Kinesiologo`; `Reporte` → `ReporteConteoLesiones` → `ReportePorZona`...; `Menu` → `MenuCoordinador`...; `DAOMemoria<T>` → `JugadorDAOMemoria`... |
| Polimorfismo | Lista de `Reporte` en `servicio/ReporteService.java` usada por `controlador/ReporteController.java`; `Menu menuDelRol = crearMenu(rol)` en `vista/MenuPrincipal.java`; `getIdentificacion()` en `Jugador` y `Kinesiologo` |
| Abstracción | Clases abstractas `Entidad`, `Persona`, `Reporte`, `Menu`, `DAOMemoria`, `FabricaDAO`; interfaces DAO en `dao/` |
| Menú de selección | Jerarquía `vista/Menu.java` (método plantilla `mostrar()`), con menús por rol |
| Estructuras condicionales y repetitivas | Regla de recurrencia en `servicio/LesionService.java` (`verificarRecurrencia`); ciclos de reintento en `Consola`; agrupamientos en `servicio/reporte/` |
| Declaración y creación de objetos; constructores | Constructores sobrecargados (con y sin id) en `modelo/`; `dao/memoria/FabricaDAOMemoria.java`; `App.java` |
| Algoritmos de ordenación y búsqueda | `util/Algoritmos.java` (inserción estable, búsqueda binaria); índice por DNI en `dao/memoria/JugadorDAOMemoria.java`; búsqueda secuencial por apellido |
| El programa compila y se ejecuta | `mvn clean package` sin advertencias; 97 pruebas; evidencias en `docs/entrega3/evidencias/` |

## Documentación

- `docs/entrega3/evidencias/`: transcripciones reales de la aplicación con sus archivos de entrada y el resumen de `mvn test`.
