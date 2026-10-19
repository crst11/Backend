#!/usr/bin/env bash
# Decide si los registros de un contenedor pueden ir a CloudWatch Logs (12 factores, factor 11, SCRUM-222). Lo carga
# desplegar.sh con "source"; no se ejecuta solo.
#
# El controlador awslogs de Docker comprueba CloudWatch AL ARRANCAR el contenedor, y si falla el contenedor no arranca. Si el
# permiso del rol no esta aplicado todavia (la plantilla servidor.yml se despliega a mano, aparte del CI), un despliegue con
# awslogs dejaria el backend sin levantar, y la vuelta a la imagen anterior tambien. Por eso antes se prueba lo mismo que
# hara el controlador, crear un stream, y solo si se puede se activa CloudWatch; si no, el contenedor sigue en json-file y
# el despliegue avisa.

# Falso si el rol de la instancia no puede escribir en el grupo (no existe, sin permiso, sin red...).
#   cloudwatch_alcanzable /cundiapp/prod/backend
#
# El stream "sondeo" se crea una vez y ya no se toca: si ya existe, AWS responde ResourceAlreadyExistsException DESPUES de
# comprobar el permiso, asi que esa respuesta tambien prueba que se puede escribir.
cloudwatch_alcanzable() {
  local salida
  if salida="$(aws logs create-log-stream --log-group-name "$1" --log-stream-name sondeo 2>&1)"; then
    return 0
  fi
  [[ "$salida" == *ResourceAlreadyExistsException* ]]
}
