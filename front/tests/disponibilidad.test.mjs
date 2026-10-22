import assert from 'node:assert/strict'
import { test } from 'node:test'
import { disponibilidad } from './api-modules.mjs'

const { MAX_BAJA, MAX_CRITICA, MAX_MEDIA, disponibilidadVigente, esAgotado, nivelDisponibilidad, textoDisponibilidad } = disponibilidad

test('availability levels split at the exported thresholds', () => {
  assert.deepEqual([MAX_CRITICA, MAX_BAJA, MAX_MEDIA], [2, 5, 10])
  const niveles = [
    [null, 'desconocida'], [-1, 'agotado'], [0, 'agotado'], [1, 'critica'], [MAX_CRITICA, 'critica'],
    [MAX_CRITICA + 1, 'baja'], [MAX_BAJA, 'baja'], [MAX_BAJA + 1, 'media'], [MAX_MEDIA, 'media'], [MAX_MEDIA + 1, 'alta'], [500, 'alta'],
  ]
  for (const [cantidad, nivel] of niveles) assert.equal(nivelDisponibilidad(cantidad), nivel, `disponibilidad ${cantidad}`)
})

test('only zero seats count as sold out; unknown availability does not block buying', () => {
  assert.equal(esAgotado(0), true)
  assert.equal(esAgotado(1), false)
  assert.equal(esAgotado(null), false)
})

test('the text always shows the seat count, with singular and sold out forms', () => {
  assert.equal(textoDisponibilidad(12), '12 lugares disponibles')
  assert.equal(textoDisponibilidad(2), '2 lugares disponibles')
  assert.equal(textoDisponibilidad(1), '1 lugar disponible')
  assert.equal(textoDisponibilidad(0), 'Agotado')
})

test('a refreshed availability wins over the package value, including a refreshed unknown', () => {
  const paquete = { id: 7, disponibilidad: 5 }
  assert.equal(disponibilidadVigente(paquete), 5)
  assert.equal(disponibilidadVigente(paquete, new Map([[8, 1]])), 5)
  assert.equal(disponibilidadVigente(paquete, new Map([[7, 4]])), 4)
  assert.equal(disponibilidadVigente(paquete, new Map([[7, 0]])), 0)
  assert.equal(disponibilidadVigente(paquete, new Map([[7, null]])), null)
})
