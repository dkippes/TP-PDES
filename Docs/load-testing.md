# Tests de carga (k6)

Los scripts están en `load-tests/` y se ejecutan con k6 dentro de Docker Compose, sin instalar nada en la máquina.

## Qué tests hay

| Test | Archivo | Qué evalúa |
| --- | --- | --- |
| Smoke | `smoke.js` | Que el flujo funciona con carga mínima (3 usuarios, 30 s). Es el que corre el workflow de GitHub. |
| Load | `load.js` | Comportamiento con la carga esperada: sube a 20 usuarios en 1 min, los sostiene 3 min y baja en 30 s. |
| Stress | `stress.js` | Sube por encima de lo esperado (20, 50, 100 y 150 usuarios) para ver dónde se degrada, y baja a 5 para ver si se recupera. |
| Spike | `spike.js` | Un salto brusco de 5 a 150 usuarios en 10 s, 1 min de pico y vuelta a 5. |

No se implementan soak (carga moderada durante horas, para detectar fugas de memoria o conexiones) porque dura horas y no entra en el alcance del TP, ni breakpoint (subir hasta que algo se rompe) porque se superpone con stress.

Todos los tests duran menos de 7 minutos. El access token de los compradores no se renueva durante el test: vence a los 15 minutos por defecto en Compose (1 hora con el `.env` de ejemplo, según `JWT_EXPIRATION_MS`).

## Qué se simula

- **Mezcla 80/20.** Cada iteración es, con 80% de probabilidad, un visitante (catálogo, búsqueda con filtros fijos, detalle y valoraciones de un paquete al azar) y, con 20%, un comprador (listar favoritos, agregar y quitar un favorito, ver historial de compras). Hay 1 segundo de pausa entre pasos, como un usuario real.
- **Pool de compradores.** En el `setup` se crean (si faltan) las cuentas `carga01@loadtest.com`, `carga02@loadtest.com`, etc., con la contraseña de desarrollo, y se obtiene un token de cada una. La creación es idempotente: si la cuenta ya existe (409) se reutiliza. Cada usuario virtual usa siempre la misma cuenta. Se usa un pool y no una sola cuenta porque las escrituras de favoritos bloquean la fila del usuario: con una sola cuenta los usuarios virtuales se serializarían y se mediría esa espera, no el sistema. El pool tiene 20 cuentas por defecto (`LOAD_BUYER_POOL`), una por usuario en smoke y load. En stress y spike hay más usuarios que cuentas y se reparten entre ellas; como solo el 20% de las iteraciones es de comprador, la espera por cuenta compartida es chica, pero existe.
- **Cuentas en la base de desarrollo.** Quedan en la base después del test. Se borran con `docker compose down -v`.
- Los requests se etiquetan con un nombre fijo (`GET /api/paquetes/{id}`) para que k6 no genere una serie por cada id.

## Umbrales

| Test | Errores (`http_req_failed`) | `http_req_duration` p95 | `checks` |
| --- | --- | --- | --- |
| Smoke y load | menos de 1% | menos de 500 ms | más de 99% |
| Stress y spike | menos de 5% | menos de 2000 ms | más de 95% |

Los umbrales de errores y tiempo se aplican solo al escenario medido (`{scenario:default}`). Así quedan afuera el registro y el login del `setup()`, que son lentos a propósito porque hashean la contraseña. El resumen general de k6 (`http_req_duration` sin filtro) sí los incluye; para comparar corridas usá el valor que aparece junto al umbral.

Si un umbral no se cumple, k6 termina con código distinto de cero y marca el umbral en rojo.

## Cómo correrlos

Con el stack ya levantado. k6 no inicia ni reinicia los servicios.

Para medir conviene la imagen runtime (el jar ya compilado, como en producción) en vez de `bootRun` con el código montado. `docker-compose.runtime.yml` cambia eso para backend y flying-service, y es lo que usan el workflow y los resultados de abajo:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml -f docker-compose.runtime.yml up -d --build --wait backend flying-service prometheus grafana
```

Con el stack de desarrollo de siempre (`docker compose up`) los tests también funcionan. Después, para correrlos:

```bash
docker compose -f docker-compose.yml -f docker-compose.loadtest.yml run --rm k6 run smoke.js
docker compose -f docker-compose.yml -f docker-compose.loadtest.yml run --rm k6 run load.js
docker compose -f docker-compose.yml -f docker-compose.loadtest.yml run --rm k6 run stress.js
docker compose -f docker-compose.yml -f docker-compose.loadtest.yml run --rm k6 run spike.js
```

Variables (en `.env` o en la línea de comandos): `LOAD_BASE_URL` (por defecto `http://backend:8080`) y `LOAD_BUYER_POOL` (por defecto 20).

El stress acepta `-e ESCALA=N`, que multiplica los escalones de usuarios para buscar el punto de quiebre: `run --rm k6 run -e ESCALA=5 stress.js` llega a 750 usuarios.

### Todos los tests de una

`load-tests/all-tests.sh` corre smoke, load, stress y spike uno atrás del otro (unos 15 minutos). Cada uno deja su reporte en `load-tests/results/<test>.html`; si uno falla sigue con el resto y al final imprime un resumen:

```bash
docker compose -f docker-compose.yml -f docker-compose.loadtest.yml run --rm --entrypoint sh k6 all-tests.sh
```

Para correr solo algunos, nombralos al final (`... k6 all-tests.sh smoke load`). Las variables se pasan con `-e` antes de `k6`: `-e ESCALA=5` para el stress grande o `-e K6_OUT=experimental-prometheus-rw` para ver las métricas en Grafana.

### Contra Kubernetes

Con el cluster local de [Docs/kubernetes.md](kubernetes.md) levantado, el backend se alcanza por el Ingress. Desde el contenedor de k6:

```bash
LOAD_BASE_URL=http://host.docker.internal docker compose -f docker-compose.yml -f docker-compose.loadtest.yml run --rm k6 run load.js
```

En otra terminal, para ver cómo escala el HPA del backend (1 a 3 réplicas, 80% de CPU):

```bash
kubectl get hpa -n aterrizar -w
```

El Ingress sirve `/api` en `http://localhost`, por eso la URL no lleva puerto.

## Cómo leer la salida de k6

- `http_req_duration`: tiempo de respuesta. Mirá `p(95)` (el 95% de los requests fue más rápido que ese valor) y `max`.
- `http_req_failed`: proporción de respuestas fallidas (estado de error o sin respuesta).
- `http_reqs`: cantidad total de requests y requests por segundo (throughput).
- `checks`: proporción de verificaciones que pasaron (estado esperado y forma de la respuesta). Un check fallido no corta el test, pero cuenta para el umbral.
- `iterations` y `vus`: iteraciones completas y usuarios virtuales.
- Los umbrales aparecen al final con una marca de cumplido o no cumplido.

## Verlo en vivo y guardar el reporte

k6 trae un dashboard web propio, sin Grafana. Para verlo mientras corre el test, publicá el puerto 5665 y abrí `http://localhost:5665`:

```bash
docker compose -f docker-compose.yml -f docker-compose.loadtest.yml run --rm -p 5665:5665 k6 run spike.js
```

Muestra en tiempo real usuarios virtuales, requests por segundo, tiempos de respuesta y errores. Se cierra cuando termina el test.

Al terminar, cada corrida deja ese mismo dashboard como **reporte HTML** en `load-tests/results/ultimo-reporte.html`, que se abre con cualquier navegador y no necesita el stack levantado. La siguiente corrida lo pisa; para conservarlo, dale otro nombre:

```bash
docker compose -f docker-compose.yml -f docker-compose.loadtest.yml run --rm -e K6_WEB_DASHBOARD_EXPORT=results/stress.html k6 run stress.js
```

La carpeta `load-tests/results/` está en `.gitignore`: los reportes son de cada corrida y no se commitean.

## Dashboard de Grafana

Con el stack de observabilidad, k6 envía sus métricas a Prometheus:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml -f docker-compose.loadtest.yml \
  run --rm k6 run -o experimental-prometheus-rw load.js
```

Abrí el dashboard **AterrizAR - Test de carga (k6)** en Grafana (Dashboards > AterrizAR). Arriba muestra lo que ve el cliente (usuarios virtuales, requests por segundo, p95 y p99, errores y checks) y abajo lo que pasa en el servidor (p95 de Spring, CPU y conexiones activas al pool de base de datos), para correlacionar el momento en que sube la latencia con el recurso que se satura. Prometheus recibe los datos porque se inicia con `--web.enable-remote-write-receiver`.

Ojo al leerlo: k6 envía sus percentiles y la tasa de checks **acumulados desde el inicio del test**, así que esos paneles no bajan cuando baja la carga. Para ver la evolución minuto a minuto (por ejemplo, si el sistema se recupera después del pico) mirá **p95 del servidor (último minuto)**, que sale del histograma de Spring. Usuarios virtuales, requests por segundo y tasa de error sí son por intervalo.

## Resultados

Corrida del 19/11/2026 con backend y flying-service en imagen runtime (`docker-compose.runtime.yml`), perfil `dev`, y k6 en la misma máquina (Docker Desktop, 7,7 GB). Datos demo: 3 paquetes. Los tiempos son los del escenario medido, sin el setup. Sirven como referencia, no como números de producción.

| Test | VUs máx. | Requests | Requests/s | p95 | Máx. | Errores | Umbrales |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Smoke | 3 | 137 | 3,6 | 185,5 ms | 507 ms | 0% | cumplidos |
| Load | 20 | 4.557 | 16,7 | 11,7 ms | 1,53 s | 0% | cumplidos |
| Stress con `ESCALA=5` | 750 | 130.009 | 328,4 | 9,9 ms | 4,77 s | 0% | cumplidos |
| Spike | 150 | 11.221 | 64,1 | 4,8 ms | 122 ms | 0% | cumplidos |

Los valores de "Requests" y "Máx." salen del resumen general de k6, que también incluye los requests del setup.

Qué se observó:

- **Smoke:** corrió apenas arrancó el stack. Su p95 (185 ms) es alto comparado con el load que vino después (12 ms), lo que apunta a una JVM recién iniciada que todavía no optimizó el código. No se midió por separado.
- **Carga esperada (20 usuarios):** p95 de 12 ms, muy por debajo del umbral de 500 ms.
- **Stress a 750 usuarios (329 requests por segundo):** sin errores y con p95 de 10 ms. Según Prometheus, la CPU del backend llegó al 10% y el pool de conexiones nunca tuvo requests esperando (como mucho 4 de 10 conexiones activas en las muestras cada 15 s). **No se encontró el punto de quiebre ni un cuello de botella.**
- **Pico aislado en el stress:** hubo un momento en que requests de todos los endpoints tardaron unos 5 s a la vez, y el backend registró lo mismo. Al afectar a todo junto parece una pausa del servidor (por ejemplo, de la JVM) más que un endpoint lento. No se investigó la causa.
- **Recuperación:** en el stress, el p95 del servidor minuto a minuto se mantuvo entre 4 y 15 ms durante toda la subida y volvió a 4-7 ms al bajar a 5 usuarios. En el spike pasó lo mismo antes, durante y después del pico. Los valores salen del panel "p95 del servidor (último minuto)".

Una corrida anterior (16/11) con el stack de desarrollo (`bootRun`) dio números parecidos, pero ahí el pool de conexiones llegó a 10 activas a 750 usuarios. Con la imagen runtime eso no se repitió, así que no se puede afirmar que el pool sea el primer cuello de botella.

Límites de estas mediciones: catálogo de solo 3 paquetes (las consultas son baratas), una sola corrida por test, cliente y servidor en la misma máquina, y métricas del servidor muestreadas cada 15 s, que pueden perder picos cortos. Para encontrar el punto de quiebre hay que subir `ESCALA`, sacar las pausas y correr k6 en otra máquina.
