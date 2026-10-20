# React + TypeScript + Vite

## Inicio y administración

Inicio muestra el catálogo completo de paquetes para visitantes y usuarios autenticados: destino, descripción breve e imagen placeholder local. Consume `GET /api/paquetes` sin JWT. Contempla carga, lista vacía y errores con reintento.

ADMINISTRADOR accede al ABM de usuarios y agencias mediante Administración en la navegación. Inicio vuelve al catálogo. Las peticiones administrativas usan el JWT en memoria; ante un 401 comparten la renovación de sesión y reintentan una vez.

El transporte JSON y los errores se comparten en `src/api/http.ts`; la renovación está en `src/api/authenticated.ts`. Las operaciones de usuarios/agencias dependen de ese transporte mediante contratos separados en `src/api/admin.ts`. Los DTOs de alta y edición son distintos en `src/types/admin.ts`: crear requiere contraseña y editar permite omitirla. Los paneles coordinan estado y acciones; formularios, tabla y detalle de usuario tienen componentes propios.

Usuarios incluye búsqueda, filtro por rol, detalle, alta, edición y baja confirmada. AGENTE requiere agencia; una contraseña vacía durante la edición conserva la actual. Agencias incluye búsqueda, alta, edición y baja confirmada. Los rechazos del backend se muestran en el formulario o confirmación correspondiente.

Con Compose y perfil `dev`, ingresá con `admin@gmail.com` / `12345678`. El backend carga automáticamente las agencias, cuentas y paquetes demo; ver [README principal](../README.md#administración-y-datos-demo).

## Gherkin de registro y login

Desde esta carpeta, ejecutá `npm run test:bdd` para probar únicamente los seis escenarios existentes de registro y login con respuestas de API simuladas. Requiere Node, las dependencias instaladas y Chrome preinstalado; Vite arranca automáticamente.

Para ver las acciones en Chrome y avanzar paso a paso con el inspector:

```powershell
npm run test:bdd:debug
```

Usá **Step over** para avanzar o **Resume** para continuar. Más detalles en [la guía de Gherkin](../Docs/gherkin.md).

## Authentication integration

Set `VITE_API_URL` to the application backend (default: `http://localhost:8080`). Registration calls `/api/auth/register` and redirects to login. Login receives a JWT and an HttpOnly refresh cookie; access tokens stay in memory. Reloading restores the session through `/api/auth/refresh`, and logout revokes the backend session.

`npm test` runs authentication/admin contract and refresh concurrency tests using Node's built-in test runner and the existing TypeScript compiler. CI runs these tests along with `npm run lint` and `npm run build`. Tests mock HTTP responses; manually verify cookie persistence with the frontend and backend running on the same hostname (for example, both on `localhost`).

## Organización del frontend

`App.tsx` coordina la navegación y los avisos de la aplicación. `hooks/useSession.ts` encapsula restauración, renovación y cierre de sesión, además de los cambios de la cuenta autenticada. `components/layout/AppLayout.tsx` presenta la estructura general y los estados de restauración.

Las páginas en `src/pages/` componen las secciones de cada pantalla. Los componentes visuales están organizados por responsabilidad: `components/home/`, `paquetes/`, `auth/`, `admin/` y `navigation/`. `components/forms/FormField.tsx` comparte el control de entrada sin cambiar las validaciones de cada formulario.

Los hooks en `hooks/auth/` y `hooks/admin/` manejan el estado y las operaciones de cada formulario o panel. `useAsyncAction` comparte el estado de envío y sus errores, mientras que `useRemoteData` mantiene la carga y el reintento. Las operaciones HTTP siguen en `api/`, con contratos separados para usuarios, agencias y paquetes.

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])

```
