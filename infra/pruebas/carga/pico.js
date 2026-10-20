// Prueba de pico (SCRUM-234): el inicio de una clase. De 5 a 100 estudiantes en 10 segundos, se quedan un minuto y se van.
// Como en el estres: no se espera perfeccion arriba, se espera que no se caiga y que se recupere.
import { crearUsuarios, jornada, usuarioDe } from './lib.js';

export const options = {
  scenarios: {
    pico: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '20s', target: 5 },
        { duration: '10s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '10s', target: 5 },
      ],
      gracefulRampDown: '30s',
    },
    recuperacion: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
      startTime: '2m10s',
    },
  },
  thresholds: {
    'http_req_failed{scenario:pico}': ['rate<0.30'],
    'http_req_failed{scenario:recuperacion}': ['rate<0.01'],
    'http_req_duration{scenario:recuperacion}': ['p(95)<800'],
  },
};

export function setup() {
  return crearUsuarios(Number(__ENV.USUARIOS) || 30);
}

export default function (datos) {
  jornada(usuarioDe(datos));
}
