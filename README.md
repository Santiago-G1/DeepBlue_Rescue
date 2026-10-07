# DeepBlue Rescue

## 1. Nombre del proyecto

DeepBlue Rescue

## 2. Descripcion breve

Capa de persistencia de una plataforma para organizaciones dedicadas al rescate
y rehabilitacion de fauna marina. El proyecto modela centros de recuperacion,
casos de rescate, animales, expedientes medicos, especialistas, areas de
experiencia y tratamientos, utilizando Java 21, Spring Boot 4, Spring Data JPA,
Hibernate, Flyway, PostgreSQL y Testcontainers.



## 3. Modelo de datos

| Tabla | Descripcion |
|---|---|
| `rescue_centers` | Centros de recuperacion de fauna marina |
| `rescue_cases` | Casos de rescate asociados a un centro |
| `animals` | Animales asociados a un caso de rescate |
| `medical_records` | Expediente medico de cada animal |
| `specialists` | Especialistas que participan en la recuperacion |
| `expertise` | Areas de experiencia (catalogo) |
| `specialist_expertise` | Tabla asociativa de la relacion N:M |
| `treatments` | Tratamientos realizados a los animales |

## 4. Relaciones

```
RescueCenter 1:N RescueCase
RescueCase    1:1 Animal
Animal        1:1 MedicalRecord
Specialist    N:M Expertise
Animal        1:N Treatment
Specialist    1:N Treatment
```

## 5. Instrucciones para ejecutar

Requisitos: Java 21, Maven y Docker.

```bash
cd deepblue-rescue
./mvnw clean compile
```

Levanta la base de datos PostgreSQL en un contenedor (ver seccion 11):

```bash
docker compose up -d
docker compose ps
```

Luego ejecuta la aplicacion:

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080` (ver seccion 12).

La aplicacion espera una base PostgreSQL. Las credenciales se configuran con
variables de entorno opcionales:

```text
DB_URL=jdbc:postgresql://localhost:5432/deepblue
DB_USER=postgres
DB_PASSWORD=postgres
```

En Windows:

```powershell
.\mvnw.cmd clean compile
```

## 6. Instrucciones para ejecutar tests

```bash
# Docker debe estar en ejecucion (Testcontainers levanta PostgreSQL real)
./mvnw clean test
```

En Windows:

```powershell
.\mvnw.cmd clean test
```

Las pruebas levantan un contenedor PostgreSQL `postgres:18-alpine` mediante
Testcontainers y `@ServiceConnection`. No se utiliza H2.

## 7. Explicacion de Flyway

Flyway es responsable de crear y evolucionar el esquema de forma versionada.
Las migraciones viven en `src/main/resources/db/migration`:

- `V1__create_schema.sql`: crea las tablas con PK, FK, UNIQUE, CHECK e indices.
- `V2__insert_expertise_catalog.sql`: inserta el catalogo inicial de expertise.
- `V3__add_tracking_device_to_animal.sql`: agrega el codigo opcional y unico del
  dispositivo GPS.

Cada migracion aplicada queda registrada en `flyway_schema_history`. Como el
esquema lo gestiona Flyway, Hibernate no crea ni actualiza tablas: se configura
`ddl-auto: validate`, de modo que Hibernate solo comprueba que sus entidades son
coherentes con el esquema existente.

## 8. Explicacion de Testcontainers

Testcontainers ejecuta los tests de integracion contra una instancia real de
PostgreSQL levantada en un contenedor Docker desechable. La clase
`PersistenceIntegrationTest` declara un contenedor `postgres:18-alpine` con
`@Container` y `@ServiceConnection`, por lo que Spring Boot configura
automaticamente el datasource apuntando a ese contenedor. Esto permite comprobar
constraints reales (UNIQUE, FK, CHECK), el comportamiento de Flyway y el
funcionamiento de JPA/Hibernate contra PostgreSQL genuino.

## 9. Query Methods implementados

- `RescueCenterRepository.findByCode(String)`
- `RescueCaseRepository.findByCaseCode(String)`
- `RescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus)`
- `RescueCaseRepository.findByRescueCenterCode(String)`
- `RescueCaseRepository.findByRescueDateAfterOrderByRescueDateDesc(LocalDate)`
- `AnimalRepository.findByAnimalCode(String)`
- `AnimalRepository.findByCommonNameContainingIgnoreCase(String)`
- `AnimalRepository.findByRescueCaseStatus(RescueStatus)`
- `AnimalRepository.findByRescueCaseRescueCenterCode(String)`
- `MedicalRecordRepository.findByAnimalId(Long)`
- `ExpertiseRepository.findByNameIgnoreCase(String)`
- `TreatmentRepository.findByAnimalIdOrderByPerformedAtAsc(Long)`

## 10. Consultas JPQL implementadas

- `RescueCaseRepository.findByCaseCodeWithAnimal(String)`: caso por codigo con
  `JOIN FETCH` del animal asociado.
- `SpecialistRepository.findActiveByExpertise(String)`: especialistas activos
  con determinada experiencia (`JOIN`, `LOWER`, `active = true`, `ORDER BY`).
- `TreatmentRepository.findPerformedBetween(start, end)`: tratamientos entre dos
  fechas (`between`, orden ASC).
- `TreatmentRepository.findByRescueCenterCode(String)`: tratamientos de animales
  de un centro (navega `Treatment -> Animal -> RescueCase -> RescueCenter`).
- `TreatmentRepository.findBySpecialistExpertise(String)`: tratamientos de
  especialistas con determinada experiencia (`JOIN`, `DISTINCT`).
- `AnimalRepository.findByStatusAndTreatmentSpecialistExpertise(RescueStatus,
  String)`: animales en un estado cuyos tratamientos fueron realizados por
  especialistas con determinada experiencia (`DISTINCT`).

Las consultas usan entidades y atributos Java, no nombres de tablas SQL ni SQL
nativo.

## Constraints probados

Las pruebas de integracion comprueban restricciones `UNIQUE` (centros y
dispositivos GPS), la clave foranea de los casos de rescate y el `CHECK` de
estados validos. Tambien se prueba el escenario integrador de una tortuga
marina, su expediente, especialista, expertise y tratamientos.

## 11. Base de datos con Docker Compose

El archivo `docker-compose.yml` define un servicio `postgres` con la imagen
`postgres:18-alpine` usando las mismas credenciales que `application.yml`:

| Parametro | Valor |
|---|---|
| Imagen | `postgres:18-alpine` |
| Contenedor | `deepblue-db` |
| Base | `deepblue` |
| Usuario | `postgres` |
| Password | `postgres` |
| Puerto | `5432:5432` |
| Volumen | `deepblue-data` (los datos sobreviven a `docker compose down`) |
| Healthcheck | `pg_isready` cada 5 segundos |

Comandos habituales:

```bash
docker compose up -d        # levanta la base de datos
docker compose ps           # estado del contenedor
docker compose logs -f      # logs de PostgreSQL
docker compose down         # detiene el contenedor (conserva el volumen)
docker compose down -v      # detiene y borra el volumen con los datos
```

La aplicacion se conecta con `DB_URL`, `DB_USER` y `DB_PASSWORD`, por lo que
con las variables por defecto no hay que configurar nada adicional. Los tests
no usan este servicio: `PersistenceIntegrationTest` levanta su propio
contenedor desechable con Testcontainers, de modo que `./mvnw clean test` y
`docker compose up -d` pueden convivir sin interferirse.

## 12. API REST (capa de controladores)

La capa HTTP esta en `com.deepblue.rescue.controller` y expone los 8 metodos
de la capa Service. Los controllers no contienen logica de negocio: solo
traducen HTTP a llamadas de Service y devuelven DTOs.

### Endpoints

| Metodo | Endpoint | Operacion | Service |
|---|---|---|---|
| GET | `/api/rescue-cases/{caseCode}` | Consultar caso | `RescueCaseService.findByCode()` |
| GET | `/api/rescue-cases?status=...` | Casos por estado | `RescueCaseService.findByStatus()` |
| PATCH | `/api/rescue-cases/{caseCode}/status` | Cambiar estado | `RescueCaseService.changeStatus()` |
| GET | `/api/animals/{animalCode}` | Consultar animal | `AnimalService.findByCode()` |
| GET | `/api/animals/in-rehabilitation` | Animales en rehabilitacion | `AnimalService.findAnimalsInRehabilitation()` |
| GET | `/api/animals/{animalCode}/treatments` | Tratamientos del animal | `TreatmentService.findByAnimalCode()` |
| GET | `/api/animals/{animalCode}/treatment-eligibility` | Elegibilidad de tratamiento | `AnimalService.canReceiveTreatment()` |
| POST | `/api/treatments` | Registrar tratamiento | `TreatmentService.register()` |

### Codigos de respuesta

| Codigo | Uso |
|---|---|
| 200 | Consulta o actualizacion correcta |
| 201 | Tratamiento creado (`POST /api/treatments`) |
| 400 | Request invalido: Bean Validation, JSON malformado o query param invalido |
| 404 | Recurso inexistente (`ResourceNotFoundException`) |
| 409 | Regla de negocio violada (`BusinessRuleException`) |
| 500 | Error inesperado (sin exponer stack trace ni detalles internos) |

### Contrato de errores

Todos los errores devuelven la misma estructura `ErrorResponse`, manejada por
`GlobalExceptionHandler` (`@RestControllerAdvice`):

```json
{
    "timestamp": "2026-10-06T19:01:00",
    "status": 404,
    "error": "Not Found",
    "message": "Animal not found: AN-999",
    "details": {}
}
```

Cuando la validacion de entrada falla, `details` indica campo por campo:

```json
{
    "timestamp": "2026-10-06T19:01:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Request validation failed",
    "details": {
        "animalCode": "Animal code is required",
        "description": "Description is required"
    }
}
```

### Ejemplo de uso

```bash
curl http://localhost:8080/api/rescue-cases/RES-2026-100
curl http://localhost:8080/api/animals/AN-2026-100/treatment-eligibility
curl -X POST http://localhost:8080/api/treatments \
  -H 'Content-Type: application/json' \
  -d '{"animalCode":"AN-2026-100","specialistCode":"SPEC-001","performedAt":"2026-08-21T09:00:00","type":"WOUND_CARE","description":"Cleaning of left front flipper injury."}'
curl -X PATCH http://localhost:8080/api/rescue-cases/RES-2026-100/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"READY_FOR_RELEASE"}'
```

### Validacion de entrada y reglas de negocio

- **Validacion de entrada** (Bean Validation en los DTOs, respondida con 400):
  `animalCode` o `specialistCode` vacios, `status` nulo, `description` fuera del
  rango 10-500 caracteres, fecha de tratamiento futura, JSON o enum invalidos.
- **Reglas de negocio** (Service, respondidas con 409) o **recursos
  inexistentes** (Service, respondidas con 404): animal o especialista
  inexistente, especialista inactivo, caso `RELEASED`/`CLOSED`, transicion de
  estado invalida o tratamiento anterior al rescate.

### Tests de controller

Los contratos HTTP se prueban con `@WebMvcTest` + `@MockitoBean` + MockMvc en
`src/test/java/com/deepblue/rescue/controller`, sin PostgreSQL ni Repository
real: `RescueCaseControllerTest`, `TreatmentControllerTest` y
`AnimalControllerTest` cubren los 18 casos minimos (200, 201, 400 por
validacion/JSON/query param, 404, 409, 500), verifican la delegacion al Service
con `verify(...)` y que la validacion no llega al Service con
`verify(..., never())`.
