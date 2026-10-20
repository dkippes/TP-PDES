## Criterios de diseño y calidad

Implementá soluciones correctas, simples, legibles y mantenibles, respetando el alcance acordado y las convenciones del proyecto.

### Alcance
- Implementá únicamente el ticket solicitado.
- Si encontrás funcionalidades relacionadas o dependencias faltantes, explicalas y consultame antes de ampliar el alcance.
- Resolvé decisiones rutinarias de implementación con criterio propio. Preguntá cuando una ambigüedad cambie el comportamiento esperado.

### Diseño
- Escribí código idiomático para el lenguaje y framework utilizados.
- Aplicá orientación a objetos cuando corresponda: encapsulación, responsabilidades claras y comportamiento cerca de los datos y reglas que le pertenecen.
- Preferí composición sobre herencia. Usá herencia cuando exista una relación de sustitución válida.
- Evitá clases, componentes o funciones que concentren demasiadas responsabilidades.

### SOLID
- SRP: cada unidad debe tener una responsabilidad cohesiva.
- OCP: facilitá extensiones cuando existan variaciones reales; evitá diseñar para necesidades hipotéticas.
- LSP: las implementaciones deben respetar los contratos y garantías de las abstracciones que reemplazan.
- ISP: definí interfaces enfocadas en las necesidades del consumidor.
- DIP: separá las reglas del negocio de los detalles externos e inyectá dependencias en las fronteras que lo necesiten.

### Simplicidad y patrones
- Aplicá KISS, YAGNI y DRY con criterio.
- Extraé código compartido cuando represente el mismo concepto o regla de negocio, no solo porque tenga una forma parecida.
- Introducí patrones, interfaces y capas cuando resuelvan un problema concreto. Evitá abstracciones especulativas.
- Usá nombres del dominio y comentarios que expliquen decisiones.

### Arquitectura y datos
- Respetá los límites entre presentación, negocio y persistencia.
- Separá contratos de transporte de modelos persistidos cuando corresponda.
- Validá entradas y protegé las invariantes en el backend.
- Cuidá permisos, transacciones, integridad y manejo de errores.
- Usá migraciones para cambios de esquema.
- Cuando se requieran datos demo, hacelos reproducibles e idempotentes.

### Frontend
- Preferí componentes pequeños y cohesivos.
- Separá acceso HTTP, estado y presentación cuando mejore la claridad.
- Extraé hooks para comportamiento reutilizable.
- Contemplá accesibilidad, carga, errores, estados vacíos y uso móvil.

### Verificación
- Agregá pruebas de comportamiento, errores y permisos relevantes.
- Evitá tests que solo reproduzcan la implementación.
- Ejecutá los checks correspondientes y revisá el diff.
- Antes de terminar, revisá acoplamiento, duplicación, complejidad y posibles regresiones.
- Informá qué cambió, qué verificaste y qué quedó pendiente.
