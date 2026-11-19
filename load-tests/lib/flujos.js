import http from "k6/http";
import exec from "k6/execution";
import { check, group, sleep } from "k6";
import { BASE_URL } from "./config.js";
import { authHeaders, prepararCompradores } from "./auth.js";

// Filtros fijos de búsqueda; se elige uno al azar en cada iteración.

const BUSQUEDAS = [
  "q=bariloche&tamanio=12",
  "q=madrid&orden=PRECIO_ASC&tamanio=12",
  "orden=PRECIO_DESC&tamanio=6",
  "orden=DESTINO_ASC&tamanio=24",
  `q=${encodeURIComponent("córdoba")}&orden=FECHA_DESC&pagina=0&tamanio=12`,
];

function elegir(lista) {
  return lista[Math.floor(Math.random() * lista.length)];
}

// Nombre estable para no multiplicar las series de métricas por cada id.
function etiqueta(name) {
  return { tags: { name } };
}

// Se ejecuta en setup(): verifica que hay datos y prepara los compradores.
export function prepararDatos() {
  // El seeder de paquetes corre después de que el backend responde: se reintenta unos segundos.
  let paqueteIds = [];
  for (let intento = 0; intento < 10 && paqueteIds.length === 0; intento++) {
    if (intento > 0) sleep(3);
    const res = http.get(
      `${BASE_URL}/api/paquetes`,
      etiqueta("GET /api/paquetes"),
    );
    if (res.status === 0) {
      exec.test.abort(
        `No se pudo conectar con ${BASE_URL} (${res.error}). Levantá el stack antes de correr el test.`,
      );
    }
    if (res.status !== 200) {
      exec.test.abort(
        `GET /api/paquetes devolvió ${res.status}. ¿Está levantado el stack?`,
      );
    }
    paqueteIds = res.json().map((p) => p.id);
  }
  if (paqueteIds.length === 0) {
    exec.test.abort(
      "No hay paquetes. Puede que el seeder los haya salteado por falta de flying-service.",
    );
  }
  return { paqueteIds, tokens: prepararCompradores() };
}

export function visitante(data) {
  const id = elegir(data.paqueteIds);

  group("catálogo", () => {
    const res = http.get(
      `${BASE_URL}/api/paquetes`,
      etiqueta("GET /api/paquetes"),
    );
    check(res, { "catálogo 200": (r) => r.status === 200 });
  });
  sleep(1);

  group("búsqueda", () => {
    const res = http.get(
      `${BASE_URL}/api/paquetes/buscar?${elegir(BUSQUEDAS)}`,
      etiqueta("GET /api/paquetes/buscar"),
    );
    check(res, {
      "búsqueda 200": (r) => r.status === 200,
      "búsqueda con resultados": (r) =>
        r.status === 200 && r.json("contenido").length > 0,
    });
  });
  sleep(1);

  group("detalle", () => {
    const res = http.get(
      `${BASE_URL}/api/paquetes/${id}`,
      etiqueta("GET /api/paquetes/{id}"),
    );
    check(res, { "detalle 200": (r) => r.status === 200 });
  });
  sleep(1);

  group("valoraciones", () => {
    const res = http.get(
      `${BASE_URL}/api/paquetes/${id}/valoraciones`,
      etiqueta("GET /api/paquetes/{id}/valoraciones"),
    );
    check(res, { "valoraciones 200": (r) => r.status === 200 });
  });
  sleep(1);
}

export function comprador(data) {
  // Cada VU usa siempre la misma cuenta. Con más VUs que cuentas, se comparten.
  const token = data.tokens[(exec.vu.idInTest - 1) % data.tokens.length];
  const params = (name) => ({ headers: authHeaders(token), tags: { name } });
  const id = elegir(data.paqueteIds);

  group("favoritos", () => {
    const lista = http.get(
      `${BASE_URL}/api/favoritos`,
      params("GET /api/favoritos"),
    );
    check(lista, { "favoritos 200": (r) => r.status === 200 });
    sleep(1);

    const alta = http.put(
      `${BASE_URL}/api/favoritos/${id}`,
      null,
      params("PUT /api/favoritos/{id}"),
    );
    check(alta, { "agregar favorito 204": (r) => r.status === 204 });
    sleep(1);

    const baja = http.del(
      `${BASE_URL}/api/favoritos/${id}`,
      null,
      params("DELETE /api/favoritos/{id}"),
    );
    check(baja, { "quitar favorito 204": (r) => r.status === 204 });
    sleep(1);
  });

  group("compras", () => {
    const res = http.get(`${BASE_URL}/api/compras`, params("GET /api/compras"));
    check(res, { "historial de compras 200": (r) => r.status === 200 });
  });
  sleep(1);
}

// Mezcla realista: 80% visitantes, 20% compradores.
export function usuarioMixto(data) {
  if (Math.random() < 0.8) {
    visitante(data);
  } else {
    comprador(data);
  }
}
