# SGSLD · Sistema de Gestión y Seguimiento de Lesiones Deportivas

Proyecto del Seminario de Práctica de Informática (Universidad Siglo 21), desarrollado con el Proceso Unificado de Desarrollo. El SGSLD centraliza el seguimiento de las lesiones de los jugadores de un club con varias categorías: registro de lesiones, tratamientos y evoluciones, alerta de posible lesión recurrente, estado de disponibilidad y reportes estadísticos.

| Rol | Puede |
|---|---|
| Coordinador | Administrar jugadores, categorías y kinesiólogos; consultar fichas y reportes |
| Kinesiólogo | Registrar lesiones, tratamientos y evoluciones; consultar jugadores y fichas |
| Cuerpo Técnico | Consultar solo el estado de disponibilidad, sin información clínica |

## Versión final (entrega 4)

- **Interfaz gráfica JavaFX** como vista principal: menú lateral por rol y las pantallas de los prototipos del informe (registrar lesión con la alerta de recurrencia, estado actual y reportes con gráfico y exportación a CSV).
- **Persistencia en MySQL 8.0** con JDBC (paquete `sgsld.dao.mysql`) sobre la base de `sgsld_script.sql`. Con `-Dsgsld.datos=memoria` se usan los DAO en memoria de la entrega 3 con el lote de prueba, sin servidor.
- La **vista de consola** de la entrega 3 se conserva (`--consola`): las dos vistas usan el mismo modelo y los mismos servicios.
- Cubre los 12 casos de uso (CU-01 a CU-12).
- Dependencias de ejecución: OpenJFX 17.0.13 (`javafx-controls`, `javafx-fxml`) y MySQL Connector/J 8.4.0, que Maven descarga de Maven Central. JUnit 5 se usa solo para las pruebas.

## Requisitos

- **JDK 17 o superior.** El código se compila con `--release 17`, de modo que el compilador rechaza cualquier API o sintaxis posterior a Java 17. Se desarrolló y probó con JDK 23, Maven 3.9.11 y MySQL 8.0.43 en Windows 11.
- **Maven 3.9**, que descarga las dependencias y arma el JAR ejecutable.
- **MySQL 8.0** con conexiones TLS habilitadas (la configuración predeterminada de MySQL 8.0) y **MySQL Workbench** o el cliente `mysql` para ejecutar los scripts. No hacen falta para el modo en memoria ni para `mvn test`.

El JAR incluye las bibliotecas de JavaFX del sistema operativo en el que se compila (Windows, en el desarrollo).

## Base de datos

Se prepara una sola vez, con un usuario administrador de MySQL (por ejemplo, `root`).

1. **Crear la base con el lote de prueba**: en Workbench, *File > Open SQL Script* con `sql/crear_base.sql` y *Query > Execute All* (o `mysql -u root -p < sql/crear_base.sql`). Ese archivo es una copia textual de las secciones 1 a 3 de `sgsld_script.sql`: crea la base `sgsld`, sus tablas, la vista `v_estado_jugador` y el lote de prueba. Si la base ya existe, la elimina y la vuelve a crear, de modo que también sirve para **reiniciar los datos**. La prueba `CrearBaseScriptTest` verifica que el bloque sea idéntico al del script. El script completo de la entrega 2 no se usa para esto porque, después del lote, ejecuta consultas, una baja, un borrado y la limpieza del lote (secciones 4 a 7).
2. **Crear el usuario de la aplicación**: abrir `sql/usuario_local.sql`, reemplazar `CAMBIAR_CLAVE` por una clave elegida por cada uno (solo en el editor, sin guardar el archivo) y ejecutarlo. Crea `'sgsld_app'@'localhost'` con `REQUIRE SSL` y solo los permisos `SELECT, INSERT, UPDATE` sobre `sgsld`, como prevé la sección 8 de `sgsld_script.sql`: sin `DELETE`, porque la aplicación solo hace bajas lógicas.
3. **Configurar la conexión**: copiar `db.properties.ejemplo` como `db.properties` en la raíz del proyecto y completar `db.clave` con la clave del paso anterior. `db.properties` está en `.gitignore` y nunca se versiona.

```properties
db.url=jdbc:mysql://localhost:3306/sgsld?sslMode=REQUIRED&connectionTimeZone=America/Argentina/Cordoba&connectTimeout=5000
db.usuario=sgsld_app
db.clave=
```

La aplicación busca `db.properties` junto al JAR y, si no está allí, en el directorio desde el que se ejecuta (la raíz del proyecto, que `mvn clean` no borra). Con `-Dsgsld.config=<ruta>` se indica otro archivo. La URL exige TLS (`sslMode=REQUIRED`), fija la zona horaria de las fechas y limita la espera de la conexión a 5 segundos.

## Compilar y ejecutar

```bash
mvn clean package
java -jar target/sgsld.jar
```

`mvn clean package` compila con todas las advertencias activadas (`-Xlint:all`) y trata cualquier advertencia como error, ejecuta las pruebas que no requieren MySQL y genera `target/sgsld.jar`, un JAR ejecutable que incluye las dependencias.

| Comando | Abre |
|---|---|
| `java -jar target/sgsld.jar` | Interfaz JavaFX con los datos de MySQL |
| `java -Dsgsld.datos=memoria -jar target/sgsld.jar` | Interfaz JavaFX con el lote de prueba en memoria (sin servidor; los cambios no se conservan al salir) |
| `java -jar target/sgsld.jar --consola` | Consola de la entrega 3 con los datos de MySQL |
| `java -Dsgsld.datos=memoria -jar target/sgsld.jar --consola` | Consola con el lote de prueba en memoria, como en la entrega 3 |
| `mvn javafx:run` | Interfaz JavaFX con los datos de MySQL, desde Maven |

Propiedades opcionales (se escriben antes de `-jar`):

| Propiedad | Efecto |
|---|---|
| `-Dsgsld.datos=mysql` o `memoria` | Origen de los datos; por defecto, `mysql`. Si MySQL no responde, la aplicación lo informa: nunca pasa en silencio a los datos en memoria |
| `-Dsgsld.hoy=2026-09-26` | Fija la fecha del sistema (formato `aaaa-MM-dd`) en la interfaz y en la consola: fechas propuestas, validaciones, edades y días transcurridos. La pantalla de inicio lo indica. Un valor inválido se informa y se usa el reloj del sistema |
| `-Dsgsld.config=<ruta>` | Archivo de configuración de la conexión en lugar de `db.properties` |
| `-Dsgsld.eco=true` | Solo en la consola: repite cada dato leído, para que las transcripciones con entrada redirigida sean legibles |

Ejemplo con la fecha del plan de pruebas, con la que los reportes dan los valores del oráculo del informe:

```bash
java -Dsgsld.hoy=2026-09-26 -jar target/sgsld.jar
```

`mvn javafx:run` ejecuta la aplicación en un proceso aparte, al que no llegan las propiedades `-D` de Maven: para usarlas, ejecutar el JAR.

### Si la aplicación no se puede conectar

Antes del inicio de sesión, la interfaz prueba la conexión. Si falla, muestra el motivo en lenguaje claro con las opciones **Reintentar** y **Salir**; la consola muestra el mismo mensaje y pregunta si se reintenta. Nunca se muestran sentencias SQL ni trazas: los detalles técnicos (SQLState, código de error y traza) quedan en `logs/sgsld.log`, dentro del directorio desde el que se ejecuta (ignorado por git).

| Mensaje (resumido) | Qué revisar |
|---|---|
| No se pudo conectar con el servidor MySQL | Que el servicio de MySQL esté en ejecución y que `db.url` tenga la dirección y el puerto correctos |
| MySQL rechazó el acceso | El usuario y la clave de `db.properties` |
| La base de datos indicada no existe | Crearla con `sql/crear_base.sql` |
| No se pudo establecer una conexión segura (TLS) | Que el servidor tenga TLS habilitado |
| No se encontró el archivo de configuración | Crear `db.properties` a partir de `db.properties.ejemplo` |

### Avisos de JavaFX al ejecutar el JAR

- Dentro de un JAR con las dependencias, JavaFX se carga desde el *classpath* y no como módulos, y por eso emite el aviso "Unsupported JavaFX configuration: classes were loaded from 'unnamed module'". Esa forma de ejecución funciona igual. La clase de inicio `sgsld.vista.fx.Lanzador` filtra solo ese aviso, en el registro `javafx`: los demás avisos de JavaFX siguen visibles. Con `mvn javafx:run`, JavaFX se carga desde el *module path*.
- El manifiesto del JAR declara `Enable-Native-Access: ALL-UNNAMED` porque JavaFX carga bibliotecas nativas: a partir de Java 24, la JVM advierte sobre el acceso nativo que no se declara. Java 17 ignora ese atributo.

## Ejecutar desde IntelliJ IDEA

1. *File > Open* y elegir el archivo `pom.xml` de la raíz (abrir como proyecto).
2. En *File > Project Structure > Project*, elegir un SDK 17 o superior.
3. Ejecutar la clase `sgsld.vista.fx.Lanzador` (interfaz JavaFX). Para la consola, agregar `--consola` en *Program arguments* o ejecutar `sgsld.App`. Las propiedades `-Dsgsld...` van en *VM options*.
4. Para las pruebas: clic derecho sobre `src/test/java` > *Run 'All Tests'*. El IDE no aplica la exclusión de Maven: también ejecuta las pruebas con la etiqueta `mysql`, que requieren `db-test.properties` (ver más abajo).

La consola de ejecución del IDE no es una terminal: la consola del SGSLD lee la contraseña con eco visible y lo avisa. Para ocultarla, ejecutar el JAR desde una terminal.

## Usuarios de prueba

La contraseña de todos los usuarios es `Sgsld2026!`.

| Usuario | Rol | Persona |
|---|---|---|
| `mlopez` | Coordinador | — |
| `lfernandez` | Kinesiólogo | Laura Fernández (MP-10234) |
| `asosa` | Kinesiólogo | Andrés Sosa (MP-11872) |
| `ctecnico` | Cuerpo Técnico | — |

Las contraseñas se guardan como hash PBKDF2-HMAC-SHA256 (600.000 iteraciones, sal de 16 bytes). Los hashes son los mismos de `sgsld_script.sql`.

## Interfaz gráfica

| Rol | Opciones del menú lateral |
|---|---|
| Coordinador | Jugadores · Categorías · Kinesiólogos · Consultar ficha · Reportes |
| Kinesiólogo | Consultar ficha · Registrar lesión · Tratamientos · Evoluciones · Reportes (sin acceso, deshabilitado) |
| Cuerpo Técnico | Estado actual |

- **Jugadores, Categorías y Kinesiólogos**: tabla, búsqueda (en Jugadores, por DNI exacto o por parte del apellido: con los datos en memoria, búsqueda binaria sobre el índice ordenado por DNI y búsqueda secuencial; con MySQL, el índice único `uq_jugador_dni` y un `LIKE` parametrizado), alta y modificación con todos los errores de validación juntos. Los jugadores tienen baja lógica con confirmación y la opción "Incluir dados de baja"; un jugador dado de baja conserva su historial.
- **Registrar lesión**: al elegir el jugador se carga su historial; si hay un antecedente en la misma zona, se muestra la alerta de posible lesión recurrente (informativa) y, después del registro, se ofrece registrar el tratamiento.
- **Tratamientos y Evoluciones**: registro del tratamiento (uno en curso por lesión), evoluciones y finalización con confirmación, duración y nuevo estado del jugador. "Registrar y finalizar" guarda la última evolución y el fin del tratamiento en una sola transacción.
- **Consultar ficha**: datos del jugador, estado, historial y, al elegir una lesión, sus tratamientos y evoluciones.
- **Estado actual**: disponibilidad de los jugadores activos, con filtro por categoría y búsqueda, sin datos clínicos.
- **Reportes**: período, criterio (seis reportes), tabla con la fila Total en los conteos y gráfico de barras horizontal. **Exportar CSV** guarda el reporte en UTF-8 con BOM y separador `;` (para Excel en español), con el nombre sugerido `reporte-<criterio>-<desde>-<hasta>.csv`.

Las operaciones contra la base se ejecutan fuera del hilo de JavaFX, sin congelar la ventana.

## Pruebas automatizadas

```bash
mvn test
```

Hay **162 pruebas JUnit 5 que no requieren MySQL**: las 97 de la entrega 3, que usan los códigos del plan de pruebas de la entrega 2 (CPC, CPI, CPS) en sus nombres, y 65 nuevas: el contrato de persistencia y de reportes con los DAO en memoria, la lectura de `db.properties`, la traducción de los errores de MySQL, la asignación de parámetros a las sentencias, la matriz de exportación de los reportes, el archivo CSV, "Registrar y finalizar", la copia fiel de `sql/crear_base.sql` y el menú por rol. Se ejecutan con el lote de prueba y un reloj fijo en el 26/09/2026.

La prueba de rendimiento (CPS02) tiene la etiqueta `rendimiento`:

```bash
mvn test -Dgroups=rendimiento
mvn test -Dsgsld.pruebas.excluidas=mysql,rendimiento
```

El primer comando ejecuta solo la prueba de rendimiento; el segundo, todas las demás que no requieren MySQL.

### Pruebas con MySQL

Las pruebas con la etiqueta `mysql` usan una base aparte, **`sgsld_test`**, nunca `sgsld`. Se preparan una sola vez:

1. Abrir `sql/usuario_pruebas.sql`, reemplazar `CAMBIAR_CLAVE` por una clave elegida (solo en el editor) y ejecutarlo con un usuario administrador. Crea `'sgsld_pruebas'@'localhost'`, con TLS obligatorio y privilegios solo sobre `sgsld_test`. No hace falta crear la base: la crean las pruebas.
2. Copiar `db-test.properties.ejemplo` como `db-test.properties` en la raíz del proyecto y completar `db.clave`. Este archivo también está en `.gitignore`.

```bash
mvn test -Dgroups=mysql
```

Antes de cada clase de prueba, `sgsld_test` se elimina y se vuelve a crear con las secciones 1 a 3 de `sgsld_script.sql` (con el nombre de la base adaptado), de modo que cada clase parte del lote original. Si `db-test.properties` no apunta a `sgsld_test`, las pruebas no se ejecutan. Son **44 pruebas**: el mismo contrato de persistencia y de reportes que con la memoria, conexión válida e inválida, el *rollback* de transacciones que fallan a mitad de camino (incluida "Registrar y finalizar"), la concurrencia entre dos puestos (un DNI o un tratamiento en curso que otro puesto registró después de la validación, un tratamiento que otro puesto ya finalizó) y los resultados de la aplicación comparados con las consultas C3 a C9 del script.

### Capturas de pantalla (herramienta de desarrollo)

```bash
java -cp target/sgsld.jar sgsld.vista.fx.Capturador
```

`Capturador` abre cada pantalla con el lote de prueba en memoria y la fecha 26/09/2026, guarda imágenes en `target/capturas/` (o en la carpeta que se indique como argumento) y recorre escenarios como lo haría un usuario, verificando sus resultados (por ejemplo, el contenido del CSV exportado). Con `-Dsgsld.datos=mysql -Dsgsld.config=db-test.properties` recorre los mismos escenarios sobre una `sgsld_test` recién creada (por ejemplo, después de `mvn test -Dgroups=mysql -Dtest=ConsultasDelScriptMySQLTest`); con cualquier otra base se niega a ejecutar, porque los escenarios registran datos.

## Consola de Windows

- La consola del SGSLD **no fuerza UTF-8**: lee con el juego de caracteres de la consola y escribe con el del sistema, de modo que las tildes se ven bien en `cmd` y PowerShell. Los símbolos de la interfaz son solo ASCII (`+ - |`, `#`, `[OK]`, `[ERROR]`, `[ALERTA]`).
- Con la entrada o la salida redirigidas en Windows, Java usa `windows-1252`. Las evidencias de `docs/entrega3/evidencias` se convirtieron a UTF-8 con `iconv`, sin otro cambio.
- PowerShell 5.1 no admite `<` para redirigir la entrada; usar Git Bash o `cmd /c "java -Dsgsld.datos=memoria -jar target\sgsld.jar --consola < archivo.txt"`.
- Al agotarse la entrada, la aplicación termina ordenadamente.

Ejemplo con la fecha del plan de pruebas y la entrada redirigida (desde Git Bash o `cmd`):

```bash
java -Dsgsld.datos=memoria -Dsgsld.eco=true -Dsgsld.hoy=2026-09-26 -jar target/sgsld.jar --consola < docs/entrega3/evidencias/entradas/03-alerta-recurrencia.txt
```

## Estructura del proyecto

```
src/main/java/sgsld/
  App.java              raíz de composición: elige la fábrica de DAO (MySQL o memoria), arma los servicios; consola
  modelo/               entidades, jerarquía Entidad/Persona, enumeraciones
  modelo/dto/           records de transporte hacia la vista
  dao/                  interfaces DAO y fábrica abstracta FabricaDAO
  dao/memoria/          DAOMemoria<T>, implementaciones en memoria, FabricaDAOMemoria y DatosDePrueba
  dao/mysql/            ConexionBD, AccesoDatosMySQL, CargadorGrafo, implementaciones JDBC y FabricaDAOMySQL
  servicio/             reglas de negocio, control de acceso por rol y el record Servicios
  servicio/reporte/     jerarquía Reporte
  controlador/          flujos de los casos de uso en la consola
  vista/                jerarquía Menu (consola)
  vista/consola/        Consola, TablaConsola y GraficoConsola (entrada y salida)
  vista/fx/             interfaz JavaFX: Lanzador, AplicacionFX, controladores de pantalla y diálogos
  util/                 SesionUsuario, HashContrasena, Algoritmos, Texto, Fechas, Validaciones,
                        ExportadorCSV, RegistroTecnico, excepciones
src/main/resources/sgsld/vista/fx/
                        un archivo FXML por pantalla y la hoja de estilo sgsld.css
src/test/java/sgsld/    pruebas JUnit 5, EscenarioPrueba y la base de pruebas MySQL
sql/                    crear_base.sql, usuario_local.sql y usuario_pruebas.sql
docs/entrega3/          evidencias de ejecución de la consola
sgsld_script.sql        base de datos MySQL 8.0 de la entrega 2 (no se modifica)
db.properties.ejemplo   modelo de la configuración de la conexión (también db-test.properties.ejemplo)
```

Dependencias entre capas: `vista` → `controlador` → `servicio` → `dao` → `modelo`; todas usan `modelo` y `util`. Las pantallas JavaFX (`vista.fx`) llaman directamente a los servicios: no acceden a los DAO ni a JDBC. Ninguna clase de `modelo`, `dao` o `servicio` lee del teclado ni escribe en la consola. `App` es la única clase que elige la implementación de la persistencia.

## Requisitos de la actividad 4 y dónde se cumplen

| Requisito | Dónde se cumple (ejemplos) |
|---|---|
| Patrón de diseño | MVC: vistas FXML, controladores JavaFX (`vista/fx/*Controller.java`) y modelo con servicios; además DAO con fábrica abstracta (`dao/FabricaDAO.java`), Singleton (`util/SesionUsuario.java`, `dao/mysql/ConexionBD.java`), método plantilla (`dao/mysql/AccesoDatosMySQL.java`) y DTO (`modelo/dto/`) |
| Persistencia y consulta en MySQL | `dao/mysql/`: `DriverManager`, `Connection`, `PreparedStatement` y `ResultSet`; transacciones en `AccesoDatosMySQL.enTransaccion`; resultados presentados en las pantallas JavaFX |
| Excepciones con MySQL | `SQLException` traducida a `util/PersistenciaException.java` (comprobada) por `dao/mysql/TraductorErrores.java`; try-with-resources; *rollback*; log técnico en `logs/sgsld.log` |
| Clases abstractas e interfaces | Interfaces DAO, `MapeadorFila<T>` y `OperacionTransaccional<R>`; clases abstractas `AccesoDatosMySQL`, `vista/fx/ControladorPantalla.java` y `ControladorAdministracion<T>`, además de `Persona`, `Reporte` y `Menu` |
| Arreglos y `ArrayList` | Arreglos: parámetros de las sentencias (`Object...`), opciones de menú (`OpcionMenu[]`), encabezados (`String[]`) y matriz de exportación (`String[][] Reporte.aMatriz`). `ArrayList`: resultados de cada consulta y listas de la interfaz |
| Archivos | Lectura de `db.properties` (`dao/mysql/ConfiguracionBD.java`), exportación a CSV (`util/ExportadorCSV.java`) y log técnico (`util/RegistroTecnico.java`) |

## Requisitos de la entrega 3 y dónde se cumplen

| Requisito | Dónde se cumple (ejemplos) |
|---|---|
| Sintaxis, tipos de datos y estructuras de control | Todo el código; por ejemplo, `LocalDate`, `Integer` opcional y `long` en `modelo/Tratamiento.java`; `switch` con flechas en `vista/MenuPrincipal.java`; `do-while` en `vista/Menu.java` |
| Tratamiento y manejo de excepciones | Jerarquía `util/SgsldException.java`; `InputMismatchException` con `finally` en `vista/consola/Consola.java` (`leerEntero`); `ValidacionException` en los controladores; red de seguridad en `Menu.mostrar()`; fin de entrada en `App.main` |
| Encapsulamiento | Atributos privados; `modelo/Tratamiento.java` sin `setEstado` ni `setFechaFin` (diagrama de estados); listas no modificables en `Lesion` y `Tratamiento` |
| Herencia | `Entidad` → `Persona` → `Jugador`/`Kinesiologo`; `Reporte` → `ReporteConteoLesiones` → `ReportePorZona`...; `Menu` → `MenuCoordinador`...; `DAOMemoria<T>` → `JugadorDAOMemoria`... |
| Polimorfismo | Lista de `Reporte` en `servicio/ReporteService.java` usada por `controlador/ReporteController.java` y `vista/fx/ReportesController.java`; `Menu menuDelRol = crearMenu(rol)` en `vista/MenuPrincipal.java`; `getIdentificacion()` en `Jugador` y `Kinesiologo` |
| Abstracción | Clases abstractas `Entidad`, `Persona`, `Reporte`, `Menu`, `DAOMemoria`, `FabricaDAO`; interfaces DAO en `dao/` |
| Menú de selección | Jerarquía `vista/Menu.java` (método plantilla `mostrar()`), con menús por rol |
| Estructuras condicionales y repetitivas | Regla de recurrencia en `servicio/LesionService.java` (`verificarRecurrencia`); ciclos de reintento en `Consola`; agrupamientos en `servicio/reporte/` |
| Declaración y creación de objetos; constructores | Constructores sobrecargados (con y sin id) en `modelo/`; `dao/memoria/FabricaDAOMemoria.java`; `App.java` |
| Algoritmos de ordenación y búsqueda | `util/Algoritmos.java` (inserción estable, búsqueda binaria); índice por DNI en `dao/memoria/JugadorDAOMemoria.java`; búsqueda secuencial por apellido |
| El programa compila y se ejecuta | `mvn clean package` sin advertencias; evidencias en `docs/entrega3/evidencias/` |

## Documentación

- `docs/entrega3/evidencias/`: transcripciones reales de la consola de la entrega 3 con sus archivos de entrada y el resumen de `mvn test` de esa entrega.
