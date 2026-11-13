# Kubernetes (cluster local)

## Qué es y alcance

Los manifiestos de `k8s/` levantan todo el stack de AterrizAR (frontend, backend, flying-service y las dos bases PostgreSQL) en un cluster local, el de Docker Desktop. Se aplican con `kubectl apply -k k8s/`. No reemplazan a Docker Compose para el desarrollo diario.

Fuera de alcance: nube, despliegue desde el CD, observabilidad dentro del cluster y estrategias canary o blue-green.

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
| ConfigMap / Secret | `k8s/config.yaml` |

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

metrics-server (necesario para el HPA):

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.9.1/components.yaml
```

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

Vite fija la URL del backend al construir, y en el cluster el frontend y el backend salen por el mismo origen (`http://localhost`) a través del Ingress. Las imágenes locales son visibles para el Kubernetes de Docker Desktop, por eso los manifiestos usan `imagePullPolicy: IfNotPresent`.

## Levantar

```bash
kubectl apply -k k8s/
kubectl get pods -n aterrizar -w
```

Cuando los pods estén en `Running` y `Ready`, abrí http://localhost. Los usuarios demo son los mismos que con Compose; ver la sección "Administración y datos demo" del `README.md`.

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

- Falta un pod y no aparece ni en `Pending`: la ResourceQuota lo rechazó. Buscá `exceeded quota` en `kubectl get events -n aterrizar` y revisá `kubectl describe resourcequota -n aterrizar`.
- Pods en `Pending`: al nodo no le alcanzan los recursos o el volumen todavía no se creó; mirá `kubectl describe pod`.
- `ImagePullBackOff`: falta construir la imagen con el tag `k8s`.
- http://localhost no responde: falta instalar el ingress controller. Si responde 404 de nginx, el controller está pero el Ingress todavía no se aplicó o no se sincronizó.
- HPA con `<unknown>` en el uso de CPU: falta metrics-server o el parche `--kubelet-insecure-tls`.
