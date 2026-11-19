// Configuración compartida de los tests de carga.
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const BUYER_POOL = Number(__ENV.BUYER_POOL || 20);
export const LOAD_PASSWORD = __ENV.LOAD_PASSWORD || '12345678';

// Los umbrales miran solo el escenario "default": excluyen el registro y login
// del setup(), que son lentos a propósito (hash de contraseña) y no son la carga medida.

// Umbrales para carga normal (smoke y load).
export const THRESHOLDS = {
  'http_req_failed{scenario:default}': ['rate<0.01'],
  'http_req_duration{scenario:default}': ['p(95)<500'],
  checks: ['rate>0.99'],
};

// Umbrales más laxos para stress y spike.
export const THRESHOLDS_LAXOS = {
  'http_req_failed{scenario:default}': ['rate<0.05'],
  'http_req_duration{scenario:default}': ['p(95)<2000'],
  checks: ['rate>0.95'],
};
