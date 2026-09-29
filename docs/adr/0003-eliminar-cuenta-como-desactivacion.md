# ADR 0003 - Eliminar cuenta como desactivación, no como borrado

- **Estado:** aceptada
- **Fecha:** 27 de septiembre de 2026
- **Incidencia:** SCRUM-64

## Contexto

RF01 necesitaba una forma de que el estudiante eliminara su cuenta. El diseño inicial que se evaluó fue borrar o desactivar las credenciales de acceso del estudiante (`credencial_acceso`), pero el esquema tiene una restricción de integridad que no aparece en el modelo entidad-relación por ser un disparador: `comprobar_metodo_de_acceso_activo()`, aplicada con `trg_credencial_metodo_activo` y `trg_estudiante_metodo_activo`, exige que todo estudiante conserve siempre al menos una fila de `credencial_acceso` con `activa = true`. Borrar o desactivar la única credencial de una cuenta con una sola forma de entrar (la mayoría de los casos) violaría esa restricción.

Además, la Ley 1581 de 2012 no exige borrar los datos de inmediato ante una solicitud de baja, y el proyecto ya tenía en el dominio un estado `INACTIVA` en `EstadoCuenta` que ningún caso de uso usaba todavía.

## Decisión

Eliminar la cuenta cambia su `estado_cuenta` a `INACTIVA` y revoca todas sus sesiones vigentes, sin tocar ni borrar `credencial_acceso`. La cuenta y sus datos siguen en la base de datos. Esto es suficiente porque `IniciarSesionServicio` e `IniciarSesionConGoogleServicio` ya rechazaban con `CuentaNoActivaException` cualquier cuenta que no estuviera `ACTIVA`, así que una cuenta `INACTIVA` queda sin ninguna forma de volver a entrar sin necesidad de tocar las credenciales. Si la persona se registra de nuevo con el mismo correo, el sistema reescribe esa misma fila en vez de crear una segunda, porque el correo institucional es único en el esquema y la fila eliminada nunca desaparece.

## Consecuencias

- Cero migración de base de datos: se reutilizó un estado que ya existía en el dominio y en la restricción `ck_estudiante_estado` desde el esquema inicial.
- El bloqueo de acceso se apoya en una regla que ya estaba probada, en vez de agregar una segunda forma de decidir si una cuenta puede entrar.
- Los datos históricos del estudiante quedan disponibles si el proyecto necesita más adelante un proceso real de anonimización o de purga definitiva, que hoy no está construido ni lo pide ningún requerimiento.
- Queda pendiente definir, si el proyecto llega a producción con datos reales, un plazo de retención y un proceso de purga para las cuentas inactivas, que hoy no tiene fecha ni mecanismo.
