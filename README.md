# School Management API

[![CI](https://github.com/Rivangel/school-management-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Rivangel/school-management-api/actions/workflows/ci.yml)

Sistema de gestión escolar construido con **Java 17** y **Spring Boot 3**. Permite administrar alumnos, maestros, materias, calificaciones y asistencia, con autenticación **JWT**, control de acceso por **roles** y generación de **boletas en PDF** con iText.

## Stack tecnológico

- Java 17
- Spring Boot 3.3 (Web, Data JPA, Security, Validation)
- Spring Security + JWT (jjwt)
- Hibernate + MySQL 8
- iText (kernel/layout) para PDF
- Swagger / OpenAPI (springdoc)
- Maven
- Docker + docker-compose
- GitHub Actions (build + tests en cada push y PR)

## Arquitectura

```
                        ┌──────────────────────────────────────────────┐
                        │                 Cliente HTTP                 │
                        │        (Postman / Swagger UI / Frontend)     │
                        └───────────────────────┬──────────────────────┘
                                                │  Authorization: Bearer <JWT>
                        ┌───────────────────────▼──────────────────────┐
                        │              Spring Security                 │
                        │   JwtFilter → valida token → carga usuario   │
                        │   Reglas por rol (ADMIN / MAESTRO / ALUMNO)  │
                        └───────────────────────┬──────────────────────┘
                                                │
      ┌──────────────┬──────────────┬───────────▼──┬───────────────┬──────────────┐
      │ Auth         │ Alumno       │ Maestro      │ Calificacion  │ Reporte      │
      │ Controller   │ Controller   │ Controller   │ /Asistencia   │ Controller   │
      └──────┬───────┴──────┬───────┴──────┬───────┴──────┬────────┴──────┬───────┘
             │              │              │              │               │
      ┌──────▼──────────────▼──────────────▼──────────────▼───────────────▼───────┐
      │                              Services                                     │
      │   Lógica de negocio + reglas de propiedad (alumno solo ve lo suyo,        │
      │   maestro solo registra en sus materias) + PdfGenerator (iText)           │
      └────────────────────────────────────┬──────────────────────────────────────┘
                                           │
      ┌────────────────────────────────────▼──────────────────────────────────────┐
      │                    Repositories (Spring Data JPA)                         │
      └────────────────────────────────────┬──────────────────────────────────────┘
                                           │
                                    ┌──────▼──────┐
                                    │   MySQL 8   │
                                    └─────────────┘
```

Estructura de paquetes:

```
src/main/java/com/escuela/
├── config/          SecurityConfig, JwtConfig, SwaggerConfig
├── controller/      Controladores REST
├── service/         Lógica de negocio
├── repository/      Interfaces Spring Data JPA
├── model/entity/    Entidades JPA
├── model/dto/       DTOs de entrada/salida (records)
├── security/        JwtUtil, JwtFilter, UserDetailsServiceImpl, CurrentUserService
├── exception/       GlobalExceptionHandler y excepciones de negocio
└── util/            PdfGenerator (boleta con iText)
```

## Roles y permisos

| Recurso | ADMIN | MAESTRO | ALUMNO |
|---|---|---|---|
| Alumnos (CRUD) | ✅ | Solo lectura | Solo `/me` |
| Maestros (CRUD) | ✅ | Solo lectura | ❌ |
| Materias (CRUD) | ✅ | Solo lectura | Solo lectura |
| Registrar calificaciones/asistencia | ✅ | Solo de **sus** materias | ❌ |
| Consultar calificaciones/asistencia | ✅ | ✅ | Solo las **propias** |
| Boleta PDF | ✅ | ✅ | Solo la **propia** |

Los maestros y alumnos se vinculan con su cuenta de usuario por **email**; `GET /api/maestros/me`
es lo que traduce esa identidad a un id cuando el cliente lo necesita.

## Cómo correr con Docker

Requisitos: Docker y Docker Compose.

```bash
# 1. Clona el repositorio y entra al directorio
# 2. (Opcional) copia las variables de entorno y ajústalas
cp .env.example .env

# 3. Levanta todo (MySQL + API)
docker-compose up --build
```

La API queda en `http://localhost:8080` y Swagger UI en:

```
http://localhost:8080/swagger-ui.html
```

Los datos de prueba (`data.sql`) se cargan automáticamente al arrancar.

### Correr sin Docker

Necesitas MySQL 8 corriendo localmente y Maven:

```bash
mvn spring-boot:run
```

La configuración se toma de variables de entorno (ver `.env.example`); si no existen se usan valores por defecto (`localhost:3306`, BD `escuela_db`, usuario `escuela_user`).

## Tests

Los tests no necesitan MySQL: los unitarios usan Mockito y los de integración levantan H2 en memoria con el perfil `test` (`src/test/resources/application-test.yml`, modo MySQL para que `data.sql` funcione igual que en producción).

Cobertura actual:

| Suite | Qué cubre |
|---|---|
| `SchoolManagementApplicationTests` | Arranque del contexto de Spring |
| `AlumnoServiceTest` | CRUD, unicidad de matrícula/email, `ResourceNotFoundException` |
| `MaestroServiceTest` | CRUD, unicidad de email (incluida capitalización al actualizar) |
| `MateriaServiceTest` | CRUD y vinculación con el maestro (`maestroId` inexistente → 404) |
| `CalificacionServiceTest` | Upsert por alumno/materia/periodo, consultas por alumno y por materia, reglas de propiedad |
| `AsistenciaServiceTest` | Upsert por alumno/materia/fecha, consulta por alumno, reglas de propiedad |
| `CurrentUserServiceTest` | Reglas de propiedad reales: ALUMNO solo ve lo suyo, MAESTRO solo registra en sus materias, ADMIN sin restricción |
| `AuthControllerIntegrationTest` | Integración de `/api/auth`: login (200 con token, 401 en credenciales malas, 400 de validación), registro (201, contraseña cifrada, email duplicado) y uso del JWT emitido en endpoints protegidos |
| `AlumnoControllerIntegrationTest` | Integración de `/api/alumnos`: CRUD completo del ADMIN contra la BD y matriz de acceso por rol (con `@WithMockUser` y con tokens JWT reales) |
| `PaginacionIntegrationTest` | Paginación y ordenamiento de los tres listados: valores por defecto, `page`/`size`, ambas direcciones de `sort`, orden anidado por maestro, página fuera de rango, `size` por encima del tope y `sort` inexistente → 400 |
| `CorsIntegrationTest` | Política CORS: preflight resuelto antes de la autenticación, verbos y cabeceras permitidos, `Content-Disposition` expuesto y rechazo de orígenes ajenos |
| `ValidacionIntegrationTest` | Validación de entrada: largo y formato del email, duplicado que sólo cambia en mayúsculas → 400, normalización al guardar, formato del periodo, rango 0–10 e ids positivos |
| `AccesoDenegadoIntegrationTest` | Códigos de las denegaciones de la cadena de seguridad, con un **servidor real** (`RANDOM_PORT`): sin rol → 403, sin token → 401. Levanta el puerto a propósito, porque el reenvío a `/error` que cambiaba el código lo hace el contenedor de servlets y MockMvc no lo ejecuta |

Matriz de acceso que fijan los tests de `/api/alumnos`:

| | Sin token | ALUMNO | MAESTRO | ADMIN |
|---|---|---|---|---|
| `GET` (listar / ficha) | 401 | 403 | ✅ | ✅ |
| `POST` · `PUT` · `DELETE` | 401 | 403 | 403 | ✅ |

La diferencia entre **401 y 403 es contractual**, no cosmética: 401 significa
«identifícate» y 403 «ya sé quién eres, pero esto no te toca». El frontend actúa
distinto en cada caso — un 401 lo lee como sesión caducada, cierra la sesión y
manda al login—, así que devolver 401 por falta de permisos echa de la aplicación
a un usuario cuya sesión sigue viva. `AccesoDenegadoIntegrationTest` lo fija
contra un servidor real.

```bash
mvn test
```

Al momento la suite son **210 tests**.

Si no tienes Maven o JDK 17 instalados, puedes correrlos en un contenedor:

```bash
docker run --rm -u $(id -u):$(id -g) \
  -e MAVEN_CONFIG=/var/maven/.m2 \
  -v "$HOME/.m2":/var/maven/.m2 -v "$PWD":/app -w /app \
  maven:3.9-eclipse-temurin-17 mvn -B -Duser.home=/var/maven test
```

## Integración continua

El workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) corre en cada `push` y cada pull request a `main` (y a mano con *Run workflow*). El badge de arriba refleja el estado de `main`.

| Job | Qué hace |
|---|---|
| **Build y tests** | `mvn -B -ntp verify` con Temurin 17: compila, corre las 182 pruebas y empaqueta el jar |
| **Imagen Docker** | `docker build` del `Dockerfile` multi-etapa; no publica en ningún registro |

Detalles que vale la pena saber:

- **No hace falta un servicio de MySQL.** Los tests unitarios usan Mockito y los de integración levantan H2 en memoria con el perfil `test`, así que el job es autocontenido.
- El caché de `~/.m2/repository` lo maneja `actions/setup-java` con la clave derivada del `pom.xml`.
- Cada corrida deja un **resumen con la tabla de tests por clase** en la página del run.
- Si algo falla se suben los XML de `target/surefire-reports/` como artefacto (7 días) para ver el stacktrace exacto; si todo pasa se sube el **jar ejecutable**.
- Dos push seguidos a la misma rama cancelan la corrida anterior (`concurrency`).
- [`dependabot.yml`](.github/dependabot.yml) revisa cada semana las dependencias de Maven y las versiones de las actions.

## Usuarios de prueba

| Email | Contraseña | Rol |
|---|---|---|
| `admin@escuela.com` | `admin123` | ADMIN |
| `juan.perez@escuela.com` | `maestro123` | MAESTRO |
| `laura.gomez@escuela.com` | `maestro123` | MAESTRO |
| `ana.lopez@escuela.com` | `alumno123` | ALUMNO |
| `carlos.ramirez@escuela.com` | `alumno123` | ALUMNO |
| `maria.torres@escuela.com` | `alumno123` | ALUMNO |

## Endpoints

Colección de Postman incluida: [`school-management-api.postman_collection.json`](school-management-api.postman_collection.json) (el login guarda el token automáticamente).

### Auth

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | Autenticación, retorna JWT |
| POST | `/api/auth/register` | Registro de usuario |

**POST /api/auth/login**

```json
// Request
{
  "email": "admin@escuela.com",
  "password": "admin123"
}

// Response 200
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "email": "admin@escuela.com",
  "nombre": "Administrador",
  "rol": "ADMIN"
}
```

**POST /api/auth/register**

```json
// Request
{
  "nombre": "Nuevo Usuario",
  "email": "nuevo@escuela.com",
  "password": "secreto123",
  "rol": "ALUMNO"
}

// Response 201: igual que login (token + datos del usuario)
```

El resto de los endpoints requieren el header `Authorization: Bearer <token>`.

### Paginación y ordenamiento

Los listados de **alumnos**, **maestros** y **materias** devuelven páginas, no arreglos. Aceptan tres parámetros opcionales:

| Parámetro | Por defecto | Notas |
|---|---|---|
| `page` | `0` | Índice de página, empieza en 0 |
| `size` | `20` | Se recorta a un máximo de `100` |
| `sort` | `apellido,nombre` (alumnos y maestros) · `nombre` (materias) | Formato `propiedad,asc\|desc`. Se puede repetir para ordenar por varias propiedades |

`sort` admite propiedades anidadas: `GET /api/materias?sort=maestro.apellido,asc`.

Si `sort` apunta a una propiedad que no existe, la respuesta es **400** con el mensaje `No se puede ordenar por 'X': la propiedad no existe`. Un parámetro que no se puede convertir a su tipo (`?maestroId=abc`, `/api/alumnos/abc`) también es **400**, no un 500: la petición vino mal, el servidor no falló.

`GET /api/materias` acepta además `maestroId` para devolver **sólo las materias de ese maestro**. El filtro lo aplica la base de datos, así que los totales de la página cuentan las materias filtradas y no todas — un filtro que se aplicara después de paginar dejaría al cliente calculando páginas que no existen. Un `maestroId` que no corresponde a ningún maestro devuelve una **página vacía** y no un 404: es un filtro, no un recurso.

```json
// GET /api/alumnos?page=0&size=2&sort=matricula,desc
{
  "content": [
    { "id": 3, "nombre": "María", "apellido": "Torres", "matricula": "A2026003", "email": "maria.torres@escuela.com", "grupo": "B" },
    { "id": 2, "nombre": "Carlos", "apellido": "Ramírez", "matricula": "A2026002", "email": "carlos.ramirez@escuela.com", "grupo": "A" }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 3,
  "totalPages": 2,
  "first": true,
  "last": false
}
```

### Alumnos

| Método | Ruta | Roles |
|---|---|---|
| GET | `/api/alumnos` (paginado) | ADMIN, MAESTRO |
| GET | `/api/alumnos/{id}` | ADMIN, MAESTRO |
| POST | `/api/alumnos` | ADMIN |
| PUT | `/api/alumnos/{id}` | ADMIN |
| DELETE | `/api/alumnos/{id}` | ADMIN |

**POST /api/alumnos**

```json
// Request
{
  "nombre": "Pedro",
  "apellido": "Sánchez",
  "matricula": "A2026004",
  "email": "pedro.sanchez@escuela.com",
  "grupo": "B"
}

// Response 201
{
  "id": 4,
  "nombre": "Pedro",
  "apellido": "Sánchez",
  "matricula": "A2026004",
  "email": "pedro.sanchez@escuela.com",
  "grupo": "B"
}
```

`GET /api/alumnos/me` devuelve el alumno **vinculado al usuario autenticado**, resuelto por
email, y es el único endpoint de esta sección abierto al ALUMNO. Sin él, el único rol que
sólo puede consultar sus propios datos era justamente el que no podía nombrarlos: las notas
y la asistencia se piden por id (`/api/calificaciones/alumno/{id}`), el token no lo lleva y
el listado de alumnos está cerrado para él precisamente porque no debe ver a los demás. No
abre ninguna puerta nueva —devuelve su propio registro, el mismo que `validarAccesoAlumno`
ya le deja leer— y va declarado **antes** de `/{id}`. Un ADMIN o un MAESTRO no tienen alumno
vinculado y reciben un 404.

### Maestros

CRUD completo en `/api/maestros` (ADMIN; lectura también para MAESTRO). `GET /api/maestros` es paginado.

`GET /api/maestros/me` devuelve el maestro **vinculado al usuario autenticado**, resuelto por
email. Existe porque el token lleva el email y el rol pero **no el id del registro**: sin él, un
MAESTRO no puede pedir sus propias materias (`GET /api/materias?maestroId=…`) porque no sabe qué
número es. Va declarado **antes** de `/{id}`, o "me" se interpretaría como un id y saldría un 400.
Un usuario sin maestro vinculado —cualquier ADMIN— recibe un **404** con esa explicación, que es
una respuesta correcta y no una avería.

```json
// POST /api/maestros — Request
{
  "nombre": "Sofía",
  "apellido": "Martínez",
  "email": "sofia.martinez@escuela.com",
  "especialidad": "Física"
}
```

### Materias

CRUD completo en `/api/materias` (ADMIN; lectura para todos los roles). `GET /api/materias` es paginado y acepta `maestroId` como filtro (`GET /api/materias?maestroId=2`).

```json
// POST /api/materias — Request
{
  "nombre": "Estructuras de Datos",
  "creditos": 8,
  "maestroId": 2
}

// Response 201
{
  "id": 4,
  "nombre": "Estructuras de Datos",
  "creditos": 8,
  "maestroId": 2,
  "maestroNombre": "Laura Gómez"
}
```

### Calificaciones

| Método | Ruta | Roles |
|---|---|---|
| POST | `/api/calificaciones` | ADMIN, MAESTRO (solo sus materias) |
| GET | `/api/calificaciones/alumno/{id}` | ADMIN, MAESTRO, ALUMNO (solo propias) |
| GET | `/api/calificaciones/materia/{id}` | ADMIN, MAESTRO |

```json
// POST /api/calificaciones — Request
{
  "alumnoId": 2,
  "materiaId": 3,
  "calificacion": 8.5,
  "periodo": "2026-1"
}

// Response 201
{
  "id": 8,
  "alumnoId": 2,
  "alumnoNombre": "Carlos Ramírez",
  "materiaId": 3,
  "materiaNombre": "Bases de Datos",
  "calificacion": 8.5,
  "periodo": "2026-1"
}
```

Si ya existe una calificación para el mismo alumno, materia y periodo, se actualiza su valor.

### Asistencia

| Método | Ruta | Roles |
|---|---|---|
| POST | `/api/asistencia` | ADMIN, MAESTRO (solo sus materias) |
| GET | `/api/asistencia/alumno/{id}` | ADMIN, MAESTRO, ALUMNO (solo propia) |
| GET | `/api/asistencia/materia/{id}?fecha=AAAA-MM-DD` | ADMIN, MAESTRO |

`GET /api/asistencia/materia/{id}` devuelve lo ya registrado de esa materia **ese día**, que
es lo que necesita saber quien va a pasar lista: sin él, la única forma de averiguarlo era
preguntar alumno por alumno, y una pantalla que no lo sabe sólo puede volver a mandarlo todo
cada vez. La `fecha` es obligatoria —sin ella la respuesta sería el historial entero de la
materia, que no lo pide nadie— y **falta responde 400**, no 500: el parámetro que falta lo
recogía el catch-all del `GlobalExceptionHandler`, la misma mentira que contaba el tipo
inválido antes del Día 18.

Leer la lista de una materia **no exige ser su maestro** (registrarla sí); lo que no puede
es leerla un ALUMNO, que vería a todo su grupo. Esa regla va **explícita** en
`SecurityConfig`: un endpoint nuevo bajo un prefijo ya usado no hereda las de sus vecinos, y
sin ella habría caído en el `anyRequest().authenticated()` del final.

```json
// POST /api/asistencia — Request
{
  "alumnoId": 1,
  "materiaId": 1,
  "fecha": "2026-07-22",
  "presente": true
}
```

### Reportes

| Método | Ruta | Roles |
|---|---|---|
| GET | `/api/reportes/boleta/{alumnoId}` | ADMIN, MAESTRO, ALUMNO (solo propia) |

Descarga un PDF (`Content-Type: application/pdf`) con los datos del alumno, la tabla de calificaciones por materia/periodo y el promedio general.

```bash
curl -H "Authorization: Bearer $TOKEN" \
     -o boleta.pdf \
     http://localhost:8080/api/reportes/boleta/1
```

### Estadísticas

| Método | Ruta | Roles |
|---|---|---|
| GET | `/api/estadisticas` | ADMIN, MAESTRO |

Indicadores agregados para el dashboard: conteos de alumnos/maestros/materias, promedio de
calificaciones y porcentaje de asistencia, generales y por materia. Es la misma frontera que
`/api/asistencia/materia/**` y `/api/calificaciones/materia/**`: agrega todo el centro, así
que el ALUMNO no tiene acceso — solo lo suyo, por las rutas que ya tiene.

Las materias sin ninguna calificación o asistencia registrada **no aparecen** en las listas
por materia, y los promedios/porcentajes generales son `null` cuando no hay ningún registro
todavía, en vez de un engañoso `0`.

```json
// GET /api/estadisticas — Response
{
  "totalAlumnos": 3,
  "totalMaestros": 2,
  "totalMaterias": 3,
  "promedioGeneral": 8.33,
  "promedioPorMateria": [
    { "materiaId": 3, "materiaNombre": "Bases de Datos", "promedio": 9.55, "cantidadCalificaciones": 2 }
  ],
  "asistenciaGeneral": { "presentes": 5, "ausentes": 2, "total": 7, "porcentaje": 71.43 },
  "asistenciaPorMateria": [
    { "materiaId": 3, "materiaNombre": "Bases de Datos", "presentes": 0, "total": 1, "porcentaje": 0.0 }
  ]
}
```

### Errores

Todas las respuestas de error tienen el mismo formato (manejadas por `GlobalExceptionHandler`):

```json
{
  "timestamp": "2026-07-23T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Alumno con id 99 no encontrado",
  "path": "/api/alumnos/99",
  "detalles": null
}
```

Los errores de validación (400) incluyen el detalle por campo en `detalles`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validación en los datos enviados",
  "path": "/api/alumnos",
  "detalles": {
    "email": "El email debe tener un formato válido",
    "grupo": "El grupo es obligatorio"
  }
}
```

### Validación de entrada

Reglas que aplican los DTOs, más allá de que los campos sean obligatorios:

| Campo | Regla |
|---|---|
| `email` | Formato válido y máximo 120 caracteres (el largo de la columna) |
| `password` | Entre 6 y 60 caracteres |
| `calificacion` | Entre 0 y 10 |
| `creditos` | Entre 1 y 20 |
| `periodo` | Formato `AAAA-S`, con `S` igual a 1 o 2 (ej. `2026-1`) |
| `fecha` (asistencia) | No puede ser futura |
| `alumnoId` · `materiaId` · `maestroId` | Números positivos |

Los campos de texto se **normalizan** antes de validarse: se recortan los espacios de los extremos y el email se pasa a minúsculas. Esto último importa porque las comprobaciones de unicidad comparan exacto: sin normalizar, `Ana@escuela.com` esquivaría el chequeo de un `ana@escuela.com` ya existente y el choque saldría como un 409 de la base en vez de un 400 con el mensaje correcto.

## CORS

La API acepta peticiones del frontend Angular. Los orígenes se configuran con `CORS_ALLOWED_ORIGINS` (separados por coma) y por defecto es `http://localhost:4200`, el `ng serve` de desarrollo.

| Aspecto | Valor |
|---|---|
| Orígenes | `app.cors.allowed-origins` — nunca `*` |
| Métodos | `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` |
| Cabeceras aceptadas | `Authorization`, `Content-Type` |
| Cabeceras expuestas | `Content-Disposition`, `Content-Type` (para descargar la boleta PDF) |
| Credenciales | Deshabilitadas: la sesión viaja en el JWT, no en cookies |
| Caché del preflight | 1 hora |

El preflight (`OPTIONS`) se resuelve **antes** de la autenticación, porque el navegador no manda la cabecera `Authorization` en esa petición.

## Notas de diseño

- **DTOs separados de las entidades**: los controladores nunca exponen entidades JPA; se usan `records` con Bean Validation.
- **Reglas de propiedad**: además de las reglas por rol en `SecurityConfig`, `CurrentUserService` valida en la capa de servicio que un alumno solo consulte sus datos y que un maestro solo registre en sus materias (vinculación por email).
- **`/api/auth/register` es público** para facilitar las pruebas; en un despliegue real la creación de usuarios con rol ADMIN/MAESTRO debería restringirse.
- **Idempotencia en cargas de datos**: `data.sql` usa `INSERT IGNORE`, por lo que reiniciar la app no duplica registros.
