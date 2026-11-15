# ERP Colegio Internacional "Little Horizons"

Sistema de Planificacion de Recursos Empresariales (ERP) educativo, de consola, desarrollado en Java con arquitectura MVC y MySQL.

## Descripcion

**Proposito.** Centralizar y automatizar los procesos academicos, administrativos y financieros de un colegio de mas de 500 estudiantes y 40 profesores, reemplazando cuadernos fisicos y hojas de calculo dispersas.

**Problemas que resuelve.**

| Problema | Solucion en el sistema |
|---|---|
| Notas perdidas en cuadernos | Registro academico en base de datos con historial completo por estudiante |
| Clubes y eventos sin registro | Catalogo de clubes con control de cupo y registro de participantes con posiciones |
| Facturacion manual y con errores | Factura electronica en PDF con numero consecutivo, CUFE y QR simulados |
| Libros y equipos "perdidos" | Inventario y prestamos con trazabilidad y reporte de vencidos |
| Profesores sin visibilidad del rendimiento | Promedios automaticos y reporte de estudiantes que necesitan apoyo |

**Funcionalidades principales.** Gestion de estudiantes, acudientes y matricula; inscripcion en materias, notas y promedios automaticos; asistencia diaria con reporte de inasistencias; profesores, carga academica y horarios sin cruces; clubes y eventos; pagos, facturas PDF y morosos; inventario y prestamos de recursos; reportes y estadisticas.

## Autores

- **Nombre completo:** _(escribe aqui tu nombre completo)_
- **Correo electronico:** _(tu correo)_
- **GitHub:** https://github.com/CamilaSalazar851
- **Programa:** Campus Campers

## Tecnologias Utilizadas

- **Lenguaje:** Java 17 o superior (usa `record`, `switch` con flechas y `Path.of`)
- **IDE:** el indicado por el instructor (el proyecto es una aplicacion Java estandar, sin Maven ni Gradle)
- **Base de datos:** MySQL 8.0 o superior
- **Librerias externas:** unicamente el driver **MySQL Connector/J** (JDBC). El PDF de la factura se genera con codigo propio, sin librerias de terceros.

## Requerimientos Previos

- JDK 17 o superior
- **MySQL 8.0 o superior** (obligatorio: el esquema usa restricciones `CHECK`, que MySQL 5.7 ignora)
- Driver MySQL Connector/J 8.x (archivo `.jar`)
- Git
- Codificacion UTF-8 en el IDE (los nombres de algunas columnas, como `año_academico`, llevan tilde y enie)

## Configuracion de la Base de Datos

### 6.1. Creacion de la base de datos

Desde MySQL Workbench, DBeaver o la terminal, ejecute el archivo completo `docs/database.sql`:

```bash
mysql -u root -p < docs/database.sql
```

El script elimina y vuelve a crear la base `erp_little_horizons`, crea las 23 tablas (seccion DDL) y la llena con datos de prueba coherentes del anio 2026 (seccion DML).

### 6.2. Configuracion de credenciales

Las credenciales no estan en el codigo. Copie la plantilla y edite sus datos:

```bash
cp config/db.properties.example config/db.properties
```

```properties
db.url=jdbc:mysql://localhost:3306/erp_little_horizons?characterEncoding=UTF-8&useSSL=false&serverTimezone=America/Bogota&allowPublicKeyRetrieval=true
db.user=root
db.password=su_contrasena
```

`config/db.properties` esta en `.gitignore` para no subir contrasenas al repositorio.

### 6.3. Instalacion del driver JDBC

1. Descargue **MySQL Connector/J** (Platform Independent) desde https://dev.mysql.com/downloads/connector/j/
2. Copie el archivo `mysql-connector-j-8.x.x.jar` dentro de la carpeta `lib/`.
3. En el IDE, agreguelo como libreria del proyecto (en IntelliJ: *File > Project Structure > Libraries > + > Java* y seleccione el jar).

## Instalacion y Ejecucion

### 7.1. Clonar el repositorio

```bash
git clone https://github.com/usuario/little-horizons-erp.git
cd little-horizons-erp
```

### 7.2. Abrir el proyecto en el IDE

Abra la carpeta como proyecto Java existente. Marque `src/` como carpeta de codigo fuente y verifique que `lib/mysql-connector-j-8.x.x.jar` este en las librerias.

### 7.3. Compilar

Desde la raiz del proyecto (Linux/macOS):

```bash
mkdir -p out
javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name "*.java")
```

En Windows (PowerShell):

```powershell
mkdir out
javac -encoding UTF-8 -cp "lib/*" -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
```

### 7.4. Ejecutar

**Importante:** ejecute siempre desde la raiz del proyecto, porque la carpeta `config/`, la carpeta `invoices/` y el archivo `error_log.txt` se resuelven de forma relativa.

```bash
java -cp "out:lib/*" com.littlehorizons.erp.Main      # Linux / macOS
java -cp "out;lib/*" com.littlehorizons.erp.Main      # Windows
```

## Estructura del Proyecto

```
little-horizons-erp/
├── docs/database.sql                 # DDL + DML (datos de prueba)
├── config/db.properties.example      # plantilla de credenciales
├── invoices/                         # facturas PDF generadas
├── error_log.txt                # registro de errores (se crea al ejecutar)
├── lib/                              # driver JDBC
└── src/com/littlehorizons/erp/
    ├── model/        # entidades y objetos de datos (Student, Teacher, Payment, Loan...)
    ├── view/         # interfaces de consola: menus, lectura y presentacion
    ├── controller/   # logica de negocio y reglas de validacion
    ├── dao/          # acceso a datos: todo el SQL vive aqui
    ├── util/         # conexion, logger, entrada validada, generador de PDF
    └── Main.java     # punto de entrada y composicion de dependencias
```

**Arquitectura MVC.** La vista solo imprime y lee datos (nunca contiene SQL ni reglas); el controlador aplica las reglas de negocio y coordina; el modelo representa los datos. Se agrego la capa `dao/` (permitida por el enunciado) para que el controlador no conozca JDBC. El flujo es siempre `View -> Controller -> DAO -> MySQL`.

| Paquete | Responsabilidad |
|---|---|
| `model` | Clases de datos sin logica de persistencia |
| `view` | `MainMenuView` y una vista por modulo (`StudentView`, `AcademicView`, `TeacherView`, `ExtracurricularView`, `FinanceView`, `ResourceView`, `ReportView`) |
| `controller` | Una clase por modulo; maneja transacciones, valida reglas y traduce errores tecnicos a mensajes claros |
| `dao` | Consultas con `PreparedStatement` |
| `util` | `DatabaseConnection`, `JdbcHelper`, `AppLogger`, `ConsoleInput`, `InvoicePdfGenerator`, `AppConfig`, `AppException` |

## Principios y Patrones de Diseño Aplicados

### 9.1. Principios SOLID

- **S - Responsabilidad unica.** `StudentDao` solo habla con la base de datos, `StudentController` solo aplica reglas (fecha de nacimiento valida, documento no duplicado) y `StudentView` solo interactua con el usuario. `InvoicePdfGenerator` solo dibuja la factura.
- **O - Abierto/cerrado.** Se agregan modulos nuevos creando su trio DAO/controlador/vista sin modificar los existentes; solo se conecta en `Main`. `JdbcHelper.query` recibe un `RowMapper` (lambda) distinto por consulta sin cambiar el helper.
- **L - Sustitucion de Liskov.** `AppException` extiende `RuntimeException` y puede usarse donde se espere una excepcion de ejecucion; los controladores la lanzan y las vistas la capturan sin saber de que modulo viene.
- **I - Segregacion de interfaces.** Las interfaces funcionales `RowMapper` y `TransactionWork` tienen un solo metodo; y cada controlador depende solo de los DAOs que necesita (por ejemplo `TeacherController` recibe `TeacherDao` y `SubjectDao`, no un DAO gigante).
- **D - Inversion de dependencias.** Los controladores reciben sus DAOs por constructor y las vistas reciben su controlador; ninguna clase hace `new` de sus colaboradores. `Main` es la unica raiz de composicion.

### 9.2. Patrones de Diseño

- **MVC:** separa interfaz, logica y datos (ver arriba).
- **DAO (Data Access Object):** aisla el SQL en `dao/`; si cambia la base de datos, solo cambian esas clases.
- **Singleton:** `DatabaseConnection` mantiene una unica instancia con la configuracion (`getInstance()`), leida una sola vez desde `config/db.properties`. Entrega una conexion nueva por operacion para poder cerrarla con `try-with-resources`.
- **Inyeccion de dependencias por constructor:** descrita en SOLID-D.
- **Strategy (via lambdas):** `RowMapper` y `TransactionWork` pasan el comportamiento variable como funcion.
- **Value Object / DTO (`record`):** `Payment`, `Loan`, `AcademicRecord`, etc. son inmutables y sin logica de persistencia.

**Uso de lambdas y Stream API.** Los promedios se calculan con `Collectors.groupingBy` y `averagingDouble` (`AcademicController.subjectAverages`, `courseAveragePerPeriod`); `ReportController.studentsNeedingSupport` encadena `filter`, `groupingBy`, `map` y `sorted`; las vistas usan `forEach` y `ifPresentOrElse`.

## Funcionalidades Principales

**CRUD completo.** Crear y consultar en todos los modulos; **editar** estudiantes, profesores, clubes y recursos; y **desactivar** (borrado logico) estudiantes (estado Retirado), profesores (Inactivo), clubes, eventos y recursos (Dado de Baja), ademas de retirar matriculas. Se eligio borrado logico y no fisico para conservar el historial academico, financiero y de prestamos, que depende de esos registros por llaves foraneas.

**Estudiantes.** Registro con datos personales; todo estudiante se crea junto con al menos un acudiente en una sola transaccion; un acudiente puede tener varios estudiantes (hermanos); matricula por grado y anio; cambio de estado (Activo, Graduado, Retirado, Suspendido).

**Academico.** Inscripcion en las materias del grado del estudiante por periodo; registro de notas 0.0 a 5.0 con un decimal (solo el profesor asignado a la materia); promedio por materia, promedio general por periodo y promedio del curso, calculados automaticamente; historial completo desde el ingreso; asistencia diaria (Presente, Ausente, Tardanza, Excusa Medica); reporte de estudiantes con mas de 3 ausencias en una semana.

**Profesores.** Directorio, asignacion de materias por anio y seccion, y horarios de clase con deteccion de cruces para un mismo profesor.

**Extracurricular.** Catalogo de clubes con cupo maximo validado dentro de una transaccion con bloqueo de fila (dos inscripciones simultaneas no pueden ocupar el ultimo cupo); retiro con fecha y motivo; calendario de eventos; participantes y posiciones (1er, 2do, 3er lugar, Participante) solo en eventos con competencia.

**Financiero.** Catalogo de conceptos; valor ajustable por estudiante (becas) y descuento automatico del 10% en pension por hermanos; registro de pagos con recibo consecutivo; **factura electronica PDF** con datos del colegio y del acudiente, detalle, totales, CUFE (SHA-384 simulado), codigo QR simulado y resolucion simulada, guardada como `invoices/factura_000123.pdf`; historial por estudiante y por acudiente; reporte de morosos (pension sin pagar hace mas de un mes).

**Recursos.** Inventario por categoria y estado; prestamos a estudiantes y profesores (el recurso debe estar Disponible); devolucion que lo libera; marca automatica de prestamos Vencidos y reporte de vencidos; cambio a mantenimiento o baja.

**Reportes.** Matriculados por grado, promedio por materia, estudiantes que necesitan apoyo (promedio menor a 3.0), inasistencias, ingresos por concepto, morosos, participacion por evento, ocupacion de clubes y prestamos vencidos.

## Manejo de Errores y Logs

- **Estrategia.** Los DAO propagan `SQLException`; el controlador la captura, la registra en el log y lanza una `AppException` con un mensaje comprensible; la vista captura `AppException` y la muestra sin cerrar la aplicacion. El usuario nunca ve una traza de error.
- **Reglas de negocio** (cupo lleno, recurso no disponible, horario cruzado, documento duplicado) tambien se lanzan como `AppException`.
- **Transacciones.** Las operaciones de varios pasos (registrar estudiante con acudiente, prestar un recurso, inscribir en un club, registrar un pago con su factura) se ejecutan con `commit` si todo sale bien y `rollback` si algo falla; por ejemplo, si no se puede escribir el PDF, el pago tampoco se guarda.
- **Logs.** `AppLogger` agrega cada evento con fecha y hora, nivel y traza completa en `error_log.txt`, en la raiz del proyecto (el archivo se crea solo la primera vez que ocurre un evento).
- **Entradas.** `ConsoleInput` valida numeros, rangos, fechas, horas, notas y montos, y vuelve a preguntar hasta recibir un dato valido.
- **Cancelar en cualquier punto.** En cualquier campo de cualquier formulario se puede escribir `cancelar` para abortar la operacion y volver al menu del modulo, sin guardar nada (`OperationCancelledException`).
- **Recursos.** Todas las consultas usan `PreparedStatement` y `try-with-resources`.

## Capturas de Pantalla

_(Agregue aqui capturas del menu principal, el registro de un estudiante, un reporte y una factura PDF generada.)_

## Problemas Conocidos y Limitaciones

- La aplicacion es de consola y no tiene autenticacion ni roles de usuario; cualquier persona que la ejecute accede a todos los modulos.
- La factura es academica: el CUFE, el QR y la resolucion son **simulados** y no tienen validez ante la DIAN. El IVA se muestra en 0% (servicio educativo).
- **Cambio al esquema del anexo:** se agrego la tabla `horarios_clase` porque el enunciado exige registrar dia y hora de cada materia y el script original no tenia donde guardarlo. El resto del esquema se respeto tal cual.
- Las notas usan `DECIMAL(2,1)` como indica el anexo; la restriccion 0.0 a 5.0 se valida ademas en la aplicacion.
- No hay borrado fisico de registros (solo desactivacion), por diseño; para eliminar datos hay que hacerlo directamente en la base de datos.
- Los prestamos se marcan como "Vencido" cuando se consulta un reporte o la lista de prestamos, no con una tarea programada.
- Los pagos de los datos de prueba no tienen PDF; las facturas se generan al registrar pagos nuevos.

## Futuras Mejoras

- Autenticacion y roles (administrador, profesor, tesoreria).
- Interfaz grafica o web sobre los mismos controladores.
- Exportar reportes a PDF y Excel.
- Pruebas unitarias con JUnit para controladores y generador de factura.
- Integracion real con un proveedor tecnologico de facturacion electronica DIAN.
- Notificaciones por correo a acudientes (morosidad, inasistencias, prestamos vencidos).

## Licencia

Proyecto academico desarrollado como parte de un programa de formacion. Todos los derechos reservados al autor. Uso educativo.

## Contacto

Consultas y sugerencias: _(tu correo)_ - https://github.com/CamilaSalazar851
