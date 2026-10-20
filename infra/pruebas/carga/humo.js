// Prueba de humo (SCRUM-234): un usuario durante 30 s. Si esto falla, no tiene sentido seguir con las demas.
import { crearUsuarios, jornada, usuarioDe } from './lib.js';

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  return crearUsuarios(3);
}

export default function (datos) {
  jornada(usuarioDe(datos));
}
