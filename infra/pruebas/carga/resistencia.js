// Prueba de resistencia (SCRUM-234): carga moderada y constante durante un rato largo (10 minutos; DURACION=30m para mas). Busca lo que
// solo aparece con el tiempo: fugas de memoria, conexiones que no se devuelven al pool, latencia que va subiendo.
import { crearUsuarios, jornada, usuarioDe } from './lib.js';

export const options = {
  vus: 10,
  duration: __ENV.DURACION || '10m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  return crearUsuarios(Number(__ENV.USUARIOS) || 30);
}

export default function (datos) {
  jornada(usuarioDe(datos));
}
