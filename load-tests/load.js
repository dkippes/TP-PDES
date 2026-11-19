// Load test: comportamiento con la carga esperada en uso normal.
// Sube a un nivel constante de usuarios y lo sostiene para medir latencia y errores.
import { THRESHOLDS } from './lib/config.js';
import { prepararDatos, usuarioMixto } from './lib/flujos.js';

const VUS_ESPERADOS = 20;

export const options = {
  stages: [
    { duration: '1m', target: VUS_ESPERADOS },
    { duration: '3m', target: VUS_ESPERADOS },
    { duration: '30s', target: 0 },
  ],
  thresholds: THRESHOLDS,
};

export const setup = prepararDatos;
export default usuarioMixto;
