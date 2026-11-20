#!/bin/sh
# Corre varios tests de carga uno atrás del otro dentro del contenedor de k6.
# Cada test deja su reporte en results/<test>.html y, si uno falla, sigue con el resto.
# Uso (desde la raíz del repo, con el stack levantado):
#   docker compose -f docker-compose.yml -f docker-compose.loadtest.yml run --rm --entrypoint sh k6 all-tests.sh
#   ... --entrypoint sh k6 all-tests.sh smoke load      (solo algunos)
# Las variables de k6 se pasan con -e de docker compose run, por ejemplo -e ESCALA=5
# o -e K6_OUT=experimental-prometheus-rw para mandar las métricas a Grafana.

TESTS="${*:-smoke load stress spike}"
RESUMEN=""
FALLARON=0

for test in $TESTS; do
  if [ ! -f "$test.js" ]; then
    echo "No existe $test.js, se saltea."
    RESUMEN="$RESUMEN\n  $test: no existe"
    FALLARON=1
    continue
  fi
  echo "========== $test =========="
  if K6_WEB_DASHBOARD_EXPORT="results/$test.html" k6 run "$test.js"; then
    RESUMEN="$RESUMEN\n  $test: aprobado (results/$test.html)"
  else
    RESUMEN="$RESUMEN\n  $test: FALLÓ (results/$test.html)"
    FALLARON=1
  fi
done

printf "\n========== Resumen ==========%b\n" "$RESUMEN"
exit $FALLARON
