// Spike test: un salto brusco de usuarios y la vuelta a la normalidad.
// Evalúa cómo reacciona el sistema al pico y si se recupera después.
import { THRESHOLDS_LAXOS } from './lib/config.js';
import { prepararDatos, usuarioMixto } from './lib/flujos.js';

const VUS_BASE = 5;
const VUS_PICO = 150;

export const options = {
  stages: [
    { duration: '30s', target: VUS_BASE },
    { duration: '10s', target: VUS_PICO },
    { duration: '1m', target: VUS_PICO },
    { duration: '10s', target: VUS_BASE },
    { duration: '1m', target: VUS_BASE },
  ],
  thresholds: THRESHOLDS_LAXOS,
};

export const setup = prepararDatos;
export default usuarioMixto;
