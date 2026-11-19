# Kubernetes (cluster local)

## Qué es y alcance

`kubectl apply -k k8s/` levanta el stack de AterrizAR en el clúster local de Docker Desktop. El frontend, el backend, flying-service y sus dos bases PostgreSQL corren en `aterrizar`. Prometheus, Grafana, Loki y Alloy corren en `aterrizar-observability`.

El alcance es el despliegue local.

## Concepto de la materia y dónde está

| Concepto | Dónde está |
| --- | --- |
| Namespace | `k8s/namespace.yaml` (`aterrizar`) |
| ResourceQuota | `k8s/namespace.yaml` (`aterrizar-quota`) |
| Pod | Plantilla de pod de cada Deployment y StatefulSet; se ven con `kubectl get pods -n aterrizar` |
| Deployment | `k8s/front.yaml`, `k8s/backend.yaml`, `k8s/flying-service.yaml` |
| Réplicas | `front` con 2 réplicas en `k8s/front.yaml`; `backend` entre 1 y 3 por el HPA |
| Probes | startup, liveness y readiness en `k8s/backend.yaml` y `k8s/flying-service.yaml`; liveness y readiness en `k8s/front.yaml`; `pg_isready` en las bases |
| Rolling update | `strategy.rollingUpdate` en los tres Deployments |
| HPA | `k8s/backend.yaml` (`HorizontalPodAutoscaler`, CPU al 80 %) |
| StatefulSet | `k8s/postgres.yaml` y `k8s/flying-postgres.yaml`, con `volumeClaimTemplates` |
| Service | un Service por componente; los de las bases son headless |
| Ingress | `k8s/ingress.yaml` |
| ConfigMap / Secret | `k8s/config.yaml` y el `secretGenerator` de `k8s/kustomization.yaml` |

## Requisitos

- Docker Desktop con Kubernetes habilitado (Settings > Kubernetes > Enable Kubernetes).
- `kubectl`.
- Puerto 80 libre.

## Preparar el cluster (una sola vez)

Ingress controller:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/cloud/deploy.yaml
```

Esperá a que el controller esté listo antes de aplicar los manifiestos; si no, el Ingress falla con un error del webhook `validate.nginx.ingress.kubernetes.io`:

```bash
kubectl wait -n ingress-nginx --for=condition=ready pod -l app.kubernetes.io/component=controller --timeout=120s
```

El proyecto ingress-nginx fue archivado en 2026 y ya no recibe actualizaciones. Se usa porque es el que se vio en la materia y alcanza para un cluster local.

metrics-server (necesario para el HPA). Para Kubernetes 1.31–1.33, incluido Docker Desktop con Kubernetes 1.32, usá la versión 0.8.1:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.8.1/components.yaml
```

Para Kubernetes 1.34 o posterior, usá la [versión compatible de metrics-server](https://github.com/kubernetes-sigs/metrics-server#compatibility-matrix).

Docker Desktop usa certificados que metrics-server no valida, así que hace falta este parche:

```bash
kubectl patch deployment metrics-server -n kube-system --type=json --patch-file k8s/setup/metrics-server-patch.json
```

El parche va en un archivo para que el comando funcione igual en PowerShell y en bash.

## Construir las imágenes

Desde la raíz del repositorio:

```bash
docker build -t aterrizar-backend:k8s ./backend
```

```bash
docker build -t aterrizar-flying-service:k8s ./flying.service
```

```bash
docker build -t aterrizar-front:k8s --build-arg VITE_API_URL=http://localhost ./front
```

```bash
docker build -t aterrizar-alloy:1.10.0-k8s ./k8s/observability
```

La imagen de Alloy usa el paquete oficial de Grafana sobre Debian.

Vite fija la URL del backend al construir. El frontend y el backend salen por el mismo origen (`http://localhost`) a través del Ingress. Kubernetes en Docker Desktop puede usar las imágenes construidas en el motor local.

## Levantar

Las credenciales no están en el repositorio. Antes de aplicar por primera vez, copiá el archivo de ejemplo a `k8s/secrets.env` (git lo ignora) y elegí los valores; Kustomize genera el Secret a partir de ese archivo:

```bash
cp k8s/secrets.env.example k8s/secrets.env
```

```bash
kubectl apply -k k8s/
kubectl get pods -n aterrizar
kubectl get pods -n aterrizar-observability
```

Cuando los pods estén en `Running` y `Ready`, abrí http://localhost. Los usuarios demo son los mismos que con Compose; ver la sección "Administración y datos demo" del `README.md`.

## Ver métricas y logs

Con Grafana en `Ready`, dejá este comando corriendo y abrí http://localhost:3005:

```bash
kubectl port-forward -n aterrizar-observability svc/grafana 3005:3000
```

Grafana trae el dashboard **AterrizAR - Overview** y las fuentes Prometheus y Loki ya configuradas. El acceso local es anónimo con rol Editor. En **Explore**, probá `up{job=~"backend|flying-service"}` en Prometheus y `{service="backend",level=~".+"}` en Loki.

Prometheus lee `/actuator/prometheus` de cada réplica de los dos servicios Spring. Alloy lee los logs de `aterrizar` a través de la API de Kubernetes y los envía a Loki. Los servicios Spring escriben JSON y sus logs se pueden filtrar por `level`. Los demás contenedores conservan su formato y se pueden filtrar por `service` y `container`. Prometheus y Loki guardan los datos en volúmenes persistentes con una retención de hasta siete días.

## Guion de demo

Ver los recursos:

```bash
kubectl get all,ingress,hpa,pvc -n aterrizar
```

Auto-regeneración: borrar un pod y ver cómo el Deployment crea otro.

```bash
kubectl delete pod -n aterrizar -l app.kubernetes.io/name=front
kubectl get pods -n aterrizar
```

Escalar a mano:

```bash
kubectl scale deployment front -n aterrizar --replicas=3
```

Rolling update y vuelta atrás:

```bash
kubectl rollout restart deployment/backend -n aterrizar
kubectl rollout status deployment/backend -n aterrizar
kubectl rollout undo deployment/backend -n aterrizar
```

HPA: mirar el autoscaler mientras se genera carga contra `http://localhost/api/paquetes`.

```bash
kubectl get hpa -n aterrizar -w
```

Logs:

```bash
kubectl logs -n aterrizar deployment/backend
```

Cuota:

```bash
kubectl describe resourcequota -n aterrizar
```

## Actualizar tras cambios de código

Reconstruir la imagen del componente modificado (los comandos de arriba) y reiniciar su Deployment:

```bash
kubectl rollout restart deployment/<componente> -n aterrizar
```

## Limpiar

```bash
kubectl delete namespace aterrizar
```

Esto también borra los datos de las bases.

## Problemas comunes

- `kubectl apply -k k8s/` falla con un error sobre `secrets.env`: falta crear ese archivo desde `k8s/secrets.env.example`.
- El backend o flying-service no logran conectarse a su base después de cambiar una clave: la clave se fija al crear el volumen. Borrá el namespace y volvé a aplicar.
- Falta un pod y no aparece ni en `Pending`: la ResourceQuota lo rechazó. Buscá `exceeded quota` en `kubectl get events -n aterrizar` y revisá `kubectl describe resourcequota -n aterrizar`.
- Pods en `Pending`: al nodo no le alcanzan los recursos o el volumen todavía no se creó; mirá `kubectl describe pod`.
- `ImagePullBackOff` en la aplicación o `ErrImageNeverPull` en Alloy: revisá que la imagen local tenga el tag que indica su manifiesto.
- http://localhost no responde: falta instalar el ingress controller. Si responde 404 de nginx, el controller está pero el Ingress todavía no se aplicó o no se sincronizó.
- HPA con `<unknown>` en el uso de CPU: falta metrics-server o el parche `--kubelet-insecure-tls`.
