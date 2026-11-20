// Smoke test: verifica que el flujo completo funciona con carga mínima
// (pocos usuarios, poco tiempo) antes de correr pruebas más pesadas.
import { THRESHOLDS } from './lib/config.js';
import { prepararDatos, usuarioMixto } from './lib/flujos.js';

const VUS = 3;
const DURACION = '30s';

export const options = {
  vus: VUS,
  duration: DURACION,
  thresholds: THRESHOLDS,
};

export const setup = prepararDatos;
export default usuarioMixto;
