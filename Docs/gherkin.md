# Gherkin para el frontend

Esta suite usa playwright-bdd para ejecutar escenarios Gherkin en español sobre los formularios React, con Chrome ya instalado. Inicia únicamente Vite: no necesita Java, Docker, base de datos ni backend.

Las respuestas de la API se simulan con page.route. Se comprueba qué datos envía el frontend y cómo muestra respuestas exitosas o errores. No se prueba que el backend cree usuarios, valide contraseñas o emita tokens reales.

## Ejecutar

Desde la raíz del proyecto:

```powershell
cd front
npm ci
npm run test:bdd
```

Si las dependencias ya están instaladas, basta npm run test:bdd. Requiere Node y Chrome preinstalado. El puerto 15174 debe estar libre. No hay que descargar navegadores.

El comando verifica los tipos, genera los tests a partir de Gherkin y ejecuta seis escenarios. El código generado en .features-gen/ no se versiona ni se edita manualmente.

Para ver el reporte:

```powershell
npx playwright show-report playwright-bdd-report
```

## Archivos y alcance

- front/bdd/registro.feature: éxito, correo duplicado y campos obligatorios vacíos.
- front/bdd/login.feature: éxito, credenciales rechazadas y correo inválido.
- front/bdd/auth.steps.ts: pasos y respuestas de API simuladas. Cada escenario tiene un contexto de navegador independiente.
- front/playwright.bdd.config.ts: configuración que levanta solo Vite.

Para agregar casos, editar los .feature y reutilizar pasos; si hacen falta pasos nuevos, implementarlos en auth.steps.ts.

Para ver Chrome y avanzar acción por acción con el inspector de Playwright:

```powershell
npm run test:bdd:debug
```

En el inspector, usar Step over para avanzar una acción o Resume para continuar. El modo debug abre el navegador y pausa la ejecución; no mueve el cursor físico del mouse. El flag está incluido en el script para evitar problemas de paso de argumentos entre npm y PowerShell.

Referencia: [documentación de Playwright BDD](https://github.com/vitalets/playwright-bdd/blob/main/docs/configuration/index.md).
