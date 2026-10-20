// Prueba de estres (SCRUM-234): sube la carga por escalones hasta bastante mas de lo razonable para esta instancia y luego la baja.
// No se espera que todo siga perfecto arriba: se espera que el servidor NO SE CAIGA (ejecutar.sh comprueba que no hubo OOM ni reinicios)
// y que, al bajar la carga, se RECUPERE (el escenario "recuperacion" tiene los mismos limites de la carga normal).
import { crearUsuarios, jornada, usuarioDe } from './lib.js';

export const options = {
  scenarios: {
    estres: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 },
        { duration: '1m', target: 40 },
        { duration: '1m', target: 60 },
        { duration: '1m', target: 80 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
    recuperacion: {
      executor: 'constant-vus',
      vus: 5,
      duration: '1m',
      startTime: '6m30s',
    },
  },
  thresholds: {
    // Arriba se tolera degradacion, no colapso.
    'http_req_failed{scenario:estres}': ['rate<0.30'],
    // Ya recuperada, vale lo mismo que la carga normal.
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
