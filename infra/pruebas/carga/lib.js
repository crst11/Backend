// Piezas comunes de las pruebas de carga (SCRUM-234): crear cuentas de verdad por la API y la jornada de un estudiante.
import http from 'k6/http';
import { check, fail, group, sleep } from 'k6';

export const BASE = __ENV.BASE_URL || 'http://backend:8080';
const MAILPIT = __ENV.MAILPIT_URL || 'http://correo:8025';
const CLAVE = 'ClaveSegura123!';
const PROGRAMA = '109964'; // Ingenieria de Sistemas, Fusagasuga

// Cuanto piensa el estudiante entre un paso y otro. En carga y resistencia vale 1 (3 a 6 s por visita); en estres y pico se baja para
// que 100 usuarios sean cientos de peticiones por segundo: con las pausas normales 100 usuarios son solo unas 25 y el servidor ni se entera.
const FACTOR_PAUSA = Number(__ENV.FACTOR_PAUSA || 1);
function pensar() {
  sleep((Math.random() * 2 + 1) * FACTOR_PAUSA);
}

// El backend corre con SALTOS_DE_PROXY=1: la IP del cliente es la ultima de X-Forwarded-For. Cada usuario virtual se presenta con la
// suya, asi los limites por IP se miden como los vivirian estudiantes distintos y no uno solo detras de un mismo equipo.
export function ipDe(n) {
  return '10.' + ((n >> 16) & 255) + '.' + ((n >> 8) & 255) + '.' + (n & 255);
}

export function cabeceras(n, token) {
  const h = { 'Content-Type': 'application/json', 'X-Forwarded-For': ipDe(n) };
  if (token) {
    h.Authorization = 'Bearer ' + token;
  }
  return h;
}

function leerCodigo(correo) {
  // El codigo sale en segundo plano: se espera a que llegue a Mailpit.
  for (let i = 0; i < 50; i++) {
    const busqueda = http.get(MAILPIT + '/api/v1/search?query=' + encodeURIComponent('to:' + correo), { tags: { name: 'mailpit' } });
    const mensajes = busqueda.status === 200 ? busqueda.json('messages') : [];
    if (mensajes && mensajes.length > 0) {
      const detalle = http.get(MAILPIT + '/api/v1/message/' + mensajes[0].ID, { tags: { name: 'mailpit' } });
      const coincidencia = String(detalle.json('Text')).match(/([0-9]{6})/);
      if (coincidencia) {
        return coincidencia[1];
      }
    }
    sleep(0.3);
  }
  return fail('No llego el codigo de verificacion de ' + correo);
}

// Una cuenta completa por el camino real: registro, codigo del correo, verificacion, inicio de sesion y programa elegido.
export function crearCuenta(n) {
  const correo = 'carga.' + Date.now() + '.' + n + '@ucundinamarca.edu.co';
  const registro = http.post(
    BASE + '/api/publico/auth/registro',
    JSON.stringify({ correo: correo, contrasena: CLAVE, nombres: 'Carga', apellidos: 'Prueba', aceptaTratamientoDatos: true }),
    { headers: cabeceras(n), tags: { name: 'setup' } });
  if (registro.status !== 201) {
    fail('Registro respondio ' + registro.status + ': ' + registro.body);
  }
  const codigo = leerCodigo(correo);
  const verificacion = http.post(BASE + '/api/publico/auth/verificacion', JSON.stringify({ correo: correo, codigo: codigo }),
    { headers: cabeceras(n), tags: { name: 'setup' } });
  if (verificacion.status !== 200) {
    fail('Verificacion respondio ' + verificacion.status + ': ' + verificacion.body);
  }
  const login = http.post(BASE + '/api/publico/auth/login', JSON.stringify({ correo: correo, contrasena: CLAVE }),
    { headers: cabeceras(n), tags: { name: 'setup' } });
  if (login.status !== 200) {
    fail('Login respondio ' + login.status + ': ' + login.body);
  }
  const token = login.json('tokenDeAcceso');
  const programa = http.put(BASE + '/api/mis/perfil-academico', JSON.stringify({ codigoPrograma: PROGRAMA }),
    { headers: cabeceras(n, token), tags: { name: 'setup' } });
  if (programa.status !== 200) {
    fail('Elegir programa respondio ' + programa.status + ': ' + programa.body);
  }
  return { n: n, correo: correo, token: token };
}

export function crearUsuarios(cuantos) {
  const usuarios = [];
  for (let i = 1; i <= cuantos; i++) {
    usuarios.push(crearCuenta(i));
  }
  return usuarios;
}

export function usuarioDe(datos) {
  return datos[(__VU - 1) % datos.length];
}

function pedir(metodo, ruta, usuario, cuerpo, nombre) {
  return http.request(metodo, BASE + ruta, cuerpo ? JSON.stringify(cuerpo) : null,
    { headers: cabeceras(usuario.n, usuario.token), tags: { name: nombre || (metodo + ' ' + ruta) } });
}

// Lo que hace un estudiante en una visita: mira la guia, su cuenta, su plan, su dia y su horario, y maneja un pendiente.
export function jornada(usuario) {
  group('guia publica', () => {
    const r = http.get(BASE + '/api/publico/guia/categorias', { headers: cabeceras(usuario.n), tags: { name: 'GET categorias' } });
    check(r, { 'categorias 200': (x) => x.status === 200 });
  });
  group('mi cuenta y mi plan', () => {
    check(pedir('GET', '/api/mis/cuenta', usuario), { 'cuenta 200': (x) => x.status === 200 });
    check(pedir('GET', '/api/mis/perfil-academico', usuario), { 'perfil 200': (x) => x.status === 200 });
  });
  pensar();
  group('mi dia y mi horario', () => {
    check(pedir('GET', '/api/mis/dia', usuario), { 'dia 200': (x) => x.status === 200 });
    check(pedir('GET', '/api/mis/horario', usuario), { 'horario 200': (x) => x.status === 200 });
  });
  pensar();
  group('mis pendientes', () => {
    const limite = new Date(Date.now() + 20 * 3600 * 1000).toISOString();
    const creado = pedir('POST', '/api/mis/pendientes', usuario, { titulo: 'Taller de carga', fechaLimite: limite, prioridad: 'alta' }, 'POST pendientes');
    check(creado, { 'pendiente creado 201': (x) => x.status === 201 });
    check(pedir('GET', '/api/mis/pendientes', usuario), { 'pendientes 200': (x) => x.status === 200 });
    if (creado.status === 201) {
      const id = creado.json('id');
      check(pedir('PATCH', '/api/mis/pendientes/' + id, usuario, { cumplido: true }, 'PATCH pendiente'), { 'marcado 200': (x) => x.status === 200 });
      check(pedir('DELETE', '/api/mis/pendientes/' + id, usuario, null, 'DELETE pendiente'), { 'borrado 204': (x) => x.status === 204 });
    }
  });
  pensar();
}
