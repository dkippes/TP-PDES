import http from 'k6/http';
import exec from 'k6/execution';
import { BASE_URL, BUYER_POOL, LOAD_PASSWORD } from './config.js';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export function authHeaders(token) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
}

function correo(i) {
  return `carga${String(i).padStart(2, '0')}@loadtest.com`;
}

// Crea (si faltan) los compradores del pool y devuelve sus access tokens.
export function prepararCompradores() {
  const tokens = [];
  for (let i = 1; i <= BUYER_POOL; i++) {
    // 409 significa que la cuenta ya existe: no es un fallo.
    http.post(
      `${BASE_URL}/api/auth/register`,
      JSON.stringify({
        nombre: 'Carga',
        apellido: `Test${i}`,
        correo: correo(i),
        direccion: 'Calle Falsa 123',
        password: LOAD_PASSWORD,
      }),
      {
        headers: JSON_HEADERS,
        tags: { name: 'POST /api/auth/register' },
        responseCallback: http.expectedStatuses(201, 409),
      },
    );
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ correo: correo(i), password: LOAD_PASSWORD }),
      { headers: JSON_HEADERS, tags: { name: 'POST /api/auth/login' } },
    );
    if (res.status !== 200) {
      exec.test.abort(`Login de ${correo(i)} falló con estado ${res.status}. Revisá que el backend esté levantado y LOAD_PASSWORD.`);
    }
    tokens.push(res.json('accessToken'));
  }
  return tokens;
}
