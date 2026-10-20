// Prueba de carga (SCRUM-234): una tarde normal, 20 estudiantes a la vez durante unos minutos. Debe cumplirse sin esfuerzo.
import { crearUsuarios, jornada, usuarioDe } from './lib.js';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '30s', target: 20 },
    { duration: '3m', target: 20 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  return crearUsuarios(Number(__ENV.USUARIOS) || 30);
}

export default function (datos) {
  jornada(usuarioDe(datos));
}
