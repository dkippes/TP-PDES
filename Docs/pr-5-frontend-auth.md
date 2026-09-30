# Integrar login y registro del frontend con la autenticación existente

Las pantallas de login y registro ahora consumen la autenticación incorporada en develop por el PR #4. Se retira la segunda implementación del backend, el seeder que restablecía contraseñas y los cambios incompatibles en el modelo y la migración V1.

- Registro mediante `POST /api/auth/register`, validación de contraseña de 8 a 72 caracteres y redirección al login después de crear la cuenta.
- Login con la respuesta `{ accessToken, tokenType, expiresIn, user }`; el access token permanece en memoria.
- Recuperación de sesión al recargar y renovación antes del vencimiento mediante la cookie HttpOnly de refresh.
- Logout que revoca la sesión del backend y maneja respuestas exitosas sin cuerpo.
- Mensajes de error y errores de validación del contrato existente.
- Pruebas del contrato HTTP, cookies, errores y concurrencia de refresh con `npm test`, incorporadas al CI.

## Validación

```text
cd front
npm run lint
npm test
npm run build

cd ../backend
gradlew.bat test build
```

Resultado local: frontend lint, 5 tests y build aprobados; los 23 tests de autenticación/autorización y el empaquetado del backend (`build -x test`) también pasan.

La suite completa del backend ejecuta 52 tests y presenta 9 fallos preexistentes en `HotelControllerTest` y `PaqueteControllerTest`: esperan respuestas del CRUD pero reciben HTTP 401. El backend resultante coincide con develop; esas pruebas y sus políticas de acceso requieren un seguimiento separado.

No se ejecutó la verificación manual en navegador ni una migración contra PostgreSQL. Las migraciones quedan idénticas a develop.

Para verificar el flujo en el navegador: crear una cuenta, iniciar sesión, recargar, comprobar la recuperación de sesión, cerrar sesión y recargar nuevamente. El registro por sí solo no debe mostrar al usuario como autenticado. No se necesitan cuentas predeterminadas.
