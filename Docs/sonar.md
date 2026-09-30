# Calidad de código con SonarQube Cloud

Panel: [SonarQube Cloud — TP-PDES](https://sonarcloud.io/project/overview?id=dkippes_TP-PDES). Ejecuciones del pipeline: [GitHub Actions — Code Quality](https://github.com/dkippes/TP-PDES/actions/workflows/sonar.yml).

El repositorio se analiza como un único proyecto AterrizAR (`dkippes_TP-PDES`): backend Kotlin, flying-service Kotlin y frontend TypeScript/React. `sonar-project.properties` define la clave, fuentes, tests, binarios, dependencias y reportes. No contiene credenciales.

El workflow `.github/workflows/sonar.yml` se ejecuta en pushes a `develop` y PR dirigidos a `develop`, sin filtros por archivos. También permite ejecución manual desde Actions. El job `Sonar Quality Gate` ejecuta tests/build del frontend, tests/cobertura de ambos servicios, publica el análisis y espera el Quality Gate. Si el gate falla o no se recibe en 300 segundos, el job falla. El pipeline conserva los reportes como artefactos.

## Configuración inicial

1. Entrá en [SonarQube Cloud](https://sonarcloud.io) con GitHub, autorizá la organización/cuenta e importá `dkippes/TP-PDES` como un único proyecto.
2. El **Project Key** `dkippes_TP-PDES` y la **Organization Key** `dkippes` ya están configurados en `sonar-project.properties`.
3. La API pública confirma que `develop` ya es la rama principal de este proyecto. El workflow está centrado en esa rama; el plan Free permite analizar la principal y PR dirigidos a ella. Si querés analizar también `main`, verificá que el plan habilite múltiples ramas antes de ampliar los eventos.
4. En **Administration → Analysis Method**, desactivá **Automatic Analysis** para usar exclusivamente el análisis de CI.
5. Generá un token de análisis con permiso sobre este proyecto.
6. En GitHub → **Settings → Secrets and variables → Actions** agregá:

Un **repository secret** llamado `SONAR_TOKEN`, con el token de análisis como valor. No hacen falta variables adicionales: las claves públicas del proyecto y la organización ya están configuradas.

El token no debe aparecer en `.env`, commits, documentación ni mensajes de chat. No necesita agregarse al Docker Compose.

7. Subí la configuración y ejecutá **Code Quality** sobre `develop` para establecer el primer análisis de la rama principal antes de analizar PR.
8. Cuando esa ejecución y el Quality Gate sean correctos, agregá el check exacto `Sonar Quality Gate` a los requeridos en la protección/ruleset de `develop`. No agregues un check genérico `ci`.

El workflow informa un error explícito si falta el token. Los PR desde forks no reciben el secreto y no ejecutan este job; ese caso requiere un análisis posterior en una rama confiable. No se usa `pull_request_target` para ejecutar código de un fork.

Esta configuración apunta a SonarQube Cloud en `https://sonarcloud.io`. Para un servidor SonarQube o la región US se debe adaptar host/organización y revisar sus capacidades de análisis de ramas.

## Cobertura

Ambos servicios usan JaCoCo 0.8.15. Al ejecutar los tests, Gradle genera:

- `build/reports/jacoco/test/jacocoTestReport.xml`, importado por Sonar.
- `build/reports/jacoco/test/html/index.html`, para ver la cobertura localmente.

La tarea `sonarDependencies` copia los JAR de las dependencias de test/runtime a `build/sonar/libraries`, que usa el scanner para resolver tipos Kotlin.

Se exige **al menos 80% de cobertura total de líneas en cada servicio por separado** mediante `jacocoTestCoverageVerification`. No es un promedio entre servicios ni una condición limitada al código nuevo. No se excluyen clases Kotlin. La tarea `check` depende de esa verificación, por lo que `check` y `build` fallan si cualquiera de los servicios queda por debajo del mínimo. Code Quality ejecuta `check` para ambos servicios antes del scanner; sus workflows de CI también lo verifican al ejecutar `build`.

El frontend tiene cinco tests de contrato que cargan módulos transpilados mediante URLs `data:`. No generan un LCOV con rutas a las fuentes TypeScript. Por decisión del proyecto, **no se exige cobertura frontend por ahora**: `sonar.coverage.exclusions=front/src/**` lo deja fuera únicamente de las métricas de cobertura, no del análisis estático. Lint, tests y build siguen siendo obligatorios. Para medirlo más adelante se necesita generar LCOV y retirar esa exclusión.

Se usa además el Quality Gate configurado en Sonar (por defecto, Sonar way). Sus métricas de cobertura combinan ambos servicios y pueden considerar solo código nuevo en PR; por eso el mínimo independiente de cada servicio se verifica en Gradle. Esta integración no modifica los umbrales del gate remoto.

## Verificación local (PowerShell)

Desde la raíz:

```powershell
cd backend
.\gradlew.bat test jacocoTestReport sonarDependencies build --no-daemon
cd ..\flying.service
.\gradlew.bat test jacocoTestReport sonarDependencies build --no-daemon
cd ..\front
npm ci
npm run lint
npm test
npm run build
cd ..
```

Estos comandos verifican los tests/build y generan los reportes; no publican análisis. Para analizar localmente con SonarScanner CLI, generá primero los reportes anteriores y ejecutá desde la raíz con `SONAR_TOKEN` en el entorno:

```powershell
sonar-scanner
```

Para verificar únicamente el mínimo de cobertura en cualquiera de los servicios, ejecutá `gradlew.bat jacocoTestCoverageVerification` (o `./gradlew jacocoTestCoverageVerification` en Linux). La tarea ejecuta los tests y genera el reporte antes de comprobar el 80%.

## Referencias

- [Acción oficial del scanner](https://github.com/SonarSource/sonarqube-scan-action)
- [Cobertura JaCoCo en Sonar](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/test-coverage/java-test-coverage)
- [Cobertura JavaScript/TypeScript](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/test-coverage/javascript-typescript-test-coverage)
- [Planes y soporte de ramas/PR](https://docs.sonarsource.com/sonarqube-cloud/administering-sonarcloud/managing-subscription/subscription-plans)
