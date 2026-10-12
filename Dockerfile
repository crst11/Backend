#
# Imagen de produccion del backend (SCRUM-70). Dos etapas: la primera compila con JDK, la segunda
# solo lleva el JRE y las capas de la aplicacion, sin Maven, sin codigo fuente y sin root.
#
#   docker build -t cundiapp-backend .
#   docker run --rm -p 8080:8080 -e PERFIL=local -e DB_URL=... cundiapp-backend

# Imagenes oficiales desde el espejo de AWS y no desde Docker Hub: los ejecutores de CI comparten IP y
# chocan con el limite de descargas anonimas de Docker Hub. Para volver a Docker Hub:
#   docker build --build-arg REGISTRO=docker.io/library .
ARG REGISTRO=public.ecr.aws/docker/library

# ---- Etapa 1: compilar ----------------------------------------------------------------------------
# --platform=$BUILDPLATFORM: Maven corre en la maquina que construye, sea cual sea la plataforma de destino.
# El jar es el mismo en ARM y en x86, asi que compilarlo bajo emulacion (QEMU) solo lo haria 10 veces mas lento.
# Solo la etapa final (el JRE y el usuario) se construye para la plataforma de la instancia.
FROM --platform=$BUILDPLATFORM ${REGISTRO}/eclipse-temurin:21-jdk-alpine AS compilar
WORKDIR /src

# Primero solo lo que define las dependencias: mientras el pom no cambie, esta capa (la lenta) se reusa.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src src
# Sin pruebas: ya corrieron en el CI (necesitan Docker para Testcontainers y aqui no hay).
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
 && mv target/cundiapp-*.jar target/app.jar \
 && java -Djarmode=tools -jar target/app.jar extract --layers --launcher --destination /capas

# ---- Etapa 2: ejecutar ----------------------------------------------------------------------------
FROM ${REGISTRO}/eclipse-temurin:21-jre-alpine

# Usuario sin privilegios: si alguien explota la aplicacion, no es root dentro del contenedor.
RUN addgroup -S cundiapp && adduser -S -G cundiapp -H -s /sbin/nologin cundiapp
WORKDIR /app

# De la capa que menos cambia a la que mas: un despliegue normal solo vuelve a subir "application".
COPY --from=compilar --chown=cundiapp:cundiapp /capas/dependencies/ ./
COPY --from=compilar --chown=cundiapp:cundiapp /capas/spring-boot-loader/ ./
COPY --from=compilar --chown=cundiapp:cundiapp /capas/snapshot-dependencies/ ./
COPY --from=compilar --chown=cundiapp:cundiapp /capas/application/ ./

USER cundiapp
EXPOSE 8080

# El contenedor es el limite de memoria: el 75 % para el heap deja lo demas para metaspace e hilos.
# ExitOnOutOfMemoryError: mejor que la tarea muera y ECS la reemplace a que siga viva y rota.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.awt.headless=true"

# readiness y no la salud agregada: esa incluye la base de datos (ver SaludIntegracionTest).
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
