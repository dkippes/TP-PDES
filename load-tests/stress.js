// Stress test: sube la carga por encima de lo esperado para encontrar el punto
// donde el sistema se degrada, y mide si se recupera al bajar la carga.
import { THRESHOLDS_LAXOS } from './lib/config.js';
import { prepararDatos, usuarioMixto } from './lib/flujos.js';

// ESCALA multiplica los escalones para buscar el punto de quiebre (ej. -e ESCALA=5).
const ESCALA = Number(__ENV.ESCALA || 1);
const VUS_1 = 20 * ESCALA;
const VUS_2 = 50 * ESCALA;
const VUS_3 = 100 * ESCALA;
const VUS_PICO = 150 * ESCALA;
const VUS_RECUPERACION = 5;

export const options = {
  stages: [
    { duration: '1m', target: VUS_1 },
    { duration: '1m', target: VUS_2 },
    { duration: '1m', target: VUS_3 },
    { duration: '1m', target: VUS_PICO },
    { duration: '1m', target: VUS_PICO },
    { duration: '30s', target: VUS_RECUPERACION },
    { duration: '1m', target: VUS_RECUPERACION },
  ],
  thresholds: THRESHOLDS_LAXOS,
};

export const setup = prepararDatos;
export default usuarioMixto;
