# School Management API

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
| Alumnos (CRUD) | ✅ | Solo lectura | ❌ |
| Maestros (CRUD) | ✅ | Solo lectura | ❌ |
| Materias (CRUD) | ✅ | Solo lectura | Solo lectura |
| Registrar calificaciones/asistencia | ✅ | Solo de **sus** materias | ❌ |
| Consultar calificaciones/asistencia | ✅ | ✅ | Solo las **propias** |
| Boleta PDF | ✅ | ✅ | Solo la **propia** |

Los maestros y alumnos se vinculan con su cuenta de usuario por **email**.

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

Matriz de acceso que fijan los tests de `/api/alumnos`:

| | Sin token | ALUMNO | MAESTRO | ADMIN |
|---|---|---|---|---|
| `GET` (listar / ficha) | 401 | 403 | ✅ | ✅ |
| `POST` · `PUT` · `DELETE` | 401 | 403 | 403 | ✅ |

```bash
mvn test
```

Al momento la suite son **148 tests**.

Si no tienes Maven o JDK 17 instalados, puedes correrlos en un contenedor:

```bash
docker run --rm -u $(id -u):$(id -g) \
  -e MAVEN_CONFIG=/var/maven/.m2 \
  -v "$HOME/.m2":/var/maven/.m2 -v "$PWD":/app -w /app \
  maven:3.9-eclipse-temurin-17 mvn -B -Duser.home=/var/maven test
```

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

Si `sort` apunta a una propiedad que no existe, la respuesta es **400** con el mensaje `No se puede ordenar por 'X': la propiedad no existe`.

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

### Maestros

CRUD completo en `/api/maestros` (ADMIN; lectura también para MAESTRO). `GET /api/maestros` es paginado.

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

CRUD completo en `/api/materias` (ADMIN; lectura para todos los roles). `GET /api/materias` es paginado.

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

Los errores de validación (400) incluyen el detalle por campo en `detalles`.

## Notas de diseño

- **DTOs separados de las entidades**: los controladores nunca exponen entidades JPA; se usan `records` con Bean Validation.
- **Reglas de propiedad**: además de las reglas por rol en `SecurityConfig`, `CurrentUserService` valida en la capa de servicio que un alumno solo consulte sus datos y que un maestro solo registre en sus materias (vinculación por email).
- **`/api/auth/register` es público** para facilitar las pruebas; en un despliegue real la creación de usuarios con rol ADMIN/MAESTRO debería restringirse.
- **Idempotencia en cargas de datos**: `data.sql` usa `INSERT IGNORE`, por lo que reiniciar la app no duplica registros.
