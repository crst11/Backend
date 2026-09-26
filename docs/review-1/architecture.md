# Arquitectura de CundiApp (lo que está implementado)

Este documento describe solo lo que existe hoy en el código de las ramas `desarrollo` y `produccion` (Review 1). Lo que viene después está marcado como tal.

## 1. Vista general

CundiApp tiene tres piezas que se despliegan por separado y solo se hablan por HTTP o por SQL:

| Pieza | Tecnología | Responsabilidad |
|---|---|---|
| Frontend | Angular 22 (PWA, componentes standalone, señales) | Mostrar y capturar datos. No calcula nada del negocio y nunca habla con la base de datos. |
| Backend | Java 21 + Spring Boot 4, arquitectura hexagonal | Reglas de negocio, seguridad (JWT) y API REST en `/api`. |
| Base de datos | PostgreSQL 16 (Docker en local, Supabase en el ambiente compartido) | Persistencia. El esquema lo crea Flyway (`V1`, `V2`, `V3`); Hibernate solo lo valida. |

```mermaid
flowchart LR
    U(["Estudiante / visitante<br/>navegador o celular"])

    subgraph FE["Frontend · Angular (localhost:4200)"]
        direction TB
        C["Componentes de pantalla<br/>features/entry, features/guide, features/account"]
        R["Servicios de datos (puertos)<br/>data-access: EstudianteRepository,<br/>CategoriaDeRecursoRepository"]
        I["core/session<br/>SessionService · sessionInterceptor · sessionGuard"]
        C --> R --> I
    end

    subgraph BE["Backend · Spring Boot (localhost:8080/api)"]
        direction TB
        CT["Controller<br/>infrastructure/adapter/in/rest"]
        UC["Use case (Service)<br/>application/service"]
        DM["Dominio<br/>domain/model: reglas de negocio"]
        PO["Puerto de salida (interfaz)<br/>application/port/out"]
        AD["Repository (adaptador JPA)<br/>infrastructure/adapter/out/persistence"]
        CT --> UC --> DM
        UC --> PO
        AD -. implementa .-> PO
        AX["Adaptadores externos<br/>adapter/out/identity · notification"]
        AX -. implementa .-> PO
    end

    GIS(["Google Identity Services<br/>API externa"])
    MAIL(["Gmail SMTP<br/>código de verificación"])

    DB[("PostgreSQL 16<br/>esquema cundiapp<br/>28 tablas")]

    U -- "HTTPS / HTML" --> C
    I -- "HTTP + JSON<br/>Authorization: Bearer JWT<br/>cookie de refresco HttpOnly" --> CT
    AD -- "JDBC / SQL" --> DB
    C -- "botón Continuar con Google<br/>entrega un ID token" --> GIS
    AX -- "HTTPS: llaves públicas (JWKS)<br/>para validar el ID token" --> GIS
    AX -- "SMTP 587 con TLS" --> MAIL
```

**Flujo:** Usuario → Frontend (componente → servicio de datos → interceptor) → API REST (controller → caso de uso → dominio → puerto → adaptador) → PostgreSQL, y la respuesta hace el camino inverso hasta la pantalla.

## 2. Backend: capas

La arquitectura es hexagonal (puertos y adaptadores). Las dependencias apuntan hacia adentro y **ArchUnit lo verifica en cada compilación**: si el dominio importara Spring o JPA, la compilación falla.

| Capa que pide la review | Paquete | Qué hay hoy |
|---|---|---|
| **Controller** | `infrastructure/adapter/in/rest` | `CuentaController`, `SesionController`, `MiCuentaController`, `VinculoConGoogleController`, `GuiaController`, DTOs (`record`) y `ManejadorExcepcionesRest` (errores RFC 9457) |
| **Service / UseCase** | `application/port/in` (contrato) y `application/service` (implementación) | `RegistrarEstudiante`, `VerificarCorreo`, `ReenviarCodigoDeVerificacion`, `IniciarSesion`, `IniciarSesionConGoogle`, `VincularGoogle`, `DesvincularGoogle`, `RenovarSesion`, `CerrarSesion`, `ConsultarMiCuenta`, `ListarCategoriasDeRecurso` |
| **Dominio** | `domain/model`, `domain/exception` | `Estudiante`, `CorreoInstitucional`, `CodigoDeVerificacion`, `Sesion`, `VinculoConGoogle`, `MetodoDeAcceso`, `CategoriaDeRecurso`: las reglas viven aquí, en Java puro |
| **Repository** | `application/port/out` (contrato) y `infrastructure/adapter/out/persistence` (JPA) | `EstudianteRepositorio`, `SesionRepositorio`, `CodigoDeVerificacionRepositorio`, `VinculoConGoogleRepositorio` y sus adaptadores con Spring Data |
| Otros adaptadores de salida | `infrastructure/adapter/out/{security,clock,notification,identity}` | bcrypt, emisión de JWT, límite de intentos, reloj, envío del código por SMTP (`EnviadorDeCodigoPorCorreo`) y verificación del ID token de Google (`VerificadorDeTokenDeGoogle`) |
| Configuración | `infrastructure/config` | Seguridad, CORS, JWT y ensamblado de los casos de uso como `@Bean` |

Por qué hay un puerto entre el caso de uso y el repositorio: el caso de uso no sabe que existe PostgreSQL. Se prueba con dobles de los puertos (sin base de datos) y el día que cambie el motor solo cambia el adaptador. Es lo que la clase 1 llama *bajo acoplamiento*.

## 2.1 APIs y servicios externos

| Servicio | Para qué | Dónde está | Qué pasa si falla |
|---|---|---|---|
| **Google Identity Services** (API externa) | Iniciar sesión con un toque (SCRUM-48). El frontend muestra el botón oficial de Google y recibe un ID token; el backend lo valida contra las llaves públicas de Google (firma RS256, emisor, destinatario = `GOOGLE_CLIENT_ID`, vigencia) y solo entonces abre la sesión de la cuenta que lo vinculó. | `VerificadorDeIdentidadExternaPort` → `adapter/out/identity/VerificadorDeTokenDeGoogle` | 503 `Servicio externo no disponible`; entrar con contraseña sigue funcionando |
| **Gmail SMTP** | Enviar el código de verificación al correo institucional (SCRUM-47). | `EnviadorDeCodigoPort` → `adapter/out/notification/EnviadorDeCodigoPorCorreo` | 503 `Correo no enviado`; se puede pedir otro código |

La cuenta de Google es solo otra forma de entrar: la identidad sigue siendo el correo institucional verificado con el código. Por eso Google se vincula desde *Mi cuenta* y no crea cuentas. El correo de la universidad es Microsoft 365; iniciar con esa cuenta (Microsoft Entra ID) sería otro adaptador del mismo puerto, pero depende de que la universidad permita autorizar aplicaciones externas.

## 3. Flujo de una petición dentro del backend: `POST /api/publico/auth/login`

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend (login)
    participant SEC as Spring Security (filtros)
    participant CT as SesionController
    participant UC as IniciarSesionServicio
    participant LIM as LimitadorDeIntentos
    participant REP as EstudianteRepositorio (JPA)
    participant CIF as CifradorBcrypt
    participant SES as SesionRepositorio (JPA)
    participant JWT as EmisorDeTokensJwt
    participant DB as PostgreSQL

    F->>SEC: POST /api/publico/auth/login {correo, contrasena}
    SEC->>CT: ruta pública: pasa sin token
    CT->>CT: valida el DTO (Bean Validation) → 400 si falta un campo
    CT->>UC: ejecutar(correo, contrasena, origen)
    UC->>LIM: ¿correo o IP bloqueados? → 429
    UC->>REP: buscarPorCorreo / contrasenaCifradaDe
    REP->>DB: SELECT estudiante, credencial_acceso
    UC->>CIF: coincide(contraseña, hash bcrypt) → 401 genérico si no
    UC->>UC: ¿cuenta activa? → 403 si está pendiente
    UC->>SES: guardar(sesión con la huella SHA-256 del refresco)
    SES->>DB: INSERT INTO sesion
    UC->>JWT: emitirAcceso (20 min, HS256)
    UC-->>CT: SesionIniciada
    CT-->>F: 200 {tokenDeAcceso, cuenta} + cookie "refresco" HttpOnly
```

Los errores del dominio (`CredencialesInvalidasException`, `CuentaNoActivaException`...) no se manejan en el controlador: los traduce `ManejadorExcepcionesRest` a una respuesta `application/problem+json` con el código HTTP correcto, y cada rechazo queda en el log.

## 4. Frontend: componentes y servicios

| Pieza | Archivo | Qué hace |
|---|---|---|
| Pantallas | `features/entry/welcome`, `features/guide/categories`, `features/account/{register,verification,login,my-account}` | Formularios y listas; solo muestran y capturan. Cada una se carga de forma diferida. |
| Servicios de consumo de API | `data-access/*.repository.ts` (contrato) y `data-access/http/*` (HttpClient) | Único lugar que conoce las URLs de la API; salen de `environment.apiUrl`. |
| Sesión | `core/session/session.service.ts` | Guarda el token de acceso **solo en memoria** (nada en localStorage). |
| Interceptor | `core/session/session.interceptor.ts` | Agrega `Authorization: Bearer` a las rutas protegidas y, ante un 401, renueva la sesión una vez. |
| Guardia | `core/session/session.guard.ts` | Impide abrir `/cuenta/mi-cuenta` sin sesión. |

Cómo se comunica con el backend: el componente llama al servicio de datos (`EstudianteRepository.iniciarSesion`), que hace el `HttpClient.post` a la API. El componente nunca usa `HttpClient` directamente: es el mismo patrón de puertos del backend y permite cambiar HTTP por IndexedDB (modo sin conexión, RF12) sin tocar la pantalla.

## 5. Decisiones que se pueden defender en la review

| Tema (clases 1 a 3) | Cómo se cumple |
|---|---|
| Separación de responsabilidades | Controller solo traduce HTTP, el caso de uso orquesta, el dominio decide, el repositorio persiste. |
| Bajo acoplamiento | Puertos (interfaces) entre capas; ArchUnit rompe la compilación si alguien se salta una capa. |
| Config (12 factors) | Nada quemado: `DB_URL`, `DB_PASSWORD`, `JWT_SECRETO`, `CORS_ORIGENES` salen del entorno; en PRE/PROD sin valor por defecto. |
| Backing services | PostgreSQL local (Docker) y Supabase son intercambiables cambiando solo `DB_URL`. |
| Processes (stateless) | La sesión es un JWT; el servidor no guarda estado de usuario en memoria (ver limitación del límite de intentos). |
| Logs | SLF4J a la salida estándar: registro, verificación, inicio y cierre de sesión, rechazos y reutilización de tokens. Nunca correos, contraseñas ni tokens. |
| Frontend desacoplado | URLs desde `environment`, servicios de datos detrás de clases abstractas, sin lógica de negocio. |

## 6. Limitaciones conocidas (se dicen en la review)

- **Límite de intentos en memoria del proceso.** Funciona con un solo backend; si se escalan varias instancias, cada una cuenta aparte. Siguiente paso: llevarlo a la base de datos o a un servicio compartido.
- **Correo por Gmail.** El código sale por SMTP con una cuenta de Gmail del proyecto (unos 500 correos al día), a través del puerto `EnviadorDeCodigoPort`. Para producción real convendría un dominio propio y un proveedor transaccional; el cambio es otro adaptador, sin tocar el caso de uso.
- **Guía institucional mantenida a mano.** Los 19 documentos oficiales se cargan con una migración (V4) y se verificaron el 26/09/2026; si la universidad cambia un enlace hay que actualizarlo con otra migración. Una revisión automática de enlaces sería un paso siguiente.
- **Se presenta en local.** El despliegue en preproducción espera la decisión de hosting y del proveedor de correo.
