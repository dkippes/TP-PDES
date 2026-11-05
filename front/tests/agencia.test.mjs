import assert from 'node:assert/strict'
import { test } from 'node:test'
import { auth, createAgenciaApi, createAuthenticatedRequest, resumirClientes, vuelos, calendario } from './api-modules.mjs'

const noop = () => {}

test('agency package CRUD, hotels and sales use the signed backend endpoints', async (t) => {
  const requests = []
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    const { pathname, search } = new URL(url)
    requests.push({ path: pathname + search, ...options })
    return options.method === 'DELETE' ? new Response(null, { status: 204 }) : new Response('[]')
  })
  const api = createAgenciaApi(createAuthenticatedRequest('token', { onSessionRenewed: noop, onSessionExpired: noop }))
  const datos = {
    precio: 1500, vueloIdaId: 1, vueloVueltaId: 2, hotelId: 3, agenciaId: 4, destino: 'Bariloche', origen: 'Buenos Aires',
    fechaInicial: '2027-12-01', fechaFinal: '2027-12-10',
  }
  await api.perfil()
  await api.hoteles()
  await api.vuelos()
  await api.ventas()
  await api.paquetes.listar(4, 2)
  await api.paquetes.crear(datos)
  await api.paquetes.editar(9, datos)
  assert.equal(await api.paquetes.eliminar(9), undefined)

  assert.deepEqual(requests.map(({ method, path }) => [method, path]), [
    ['GET', '/api/usuarios/me'], ['GET', '/api/hoteles'], ['GET', '/api/vuelos'], ['GET', '/api/ventas'],
    ['GET', '/api/paquetes/buscar?agenciaId=4&orden=FECHA_DESC&pagina=2&tamanio=12'],
    ['POST', '/api/paquetes'], ['PUT', '/api/paquetes/9'], ['DELETE', '/api/paquetes/9'],
  ])
  for (const request of requests) assert.equal(request.headers.Authorization, 'Bearer token')
  assert.deepEqual(JSON.parse(requests[5].body), datos)
})

test('registering an agency posts the user and the agency to the public endpoint', async (t) => {
  let request
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    request = { path: new URL(url).pathname, ...options }
    return new Response(JSON.stringify({ id: 1, role: 'AGENTE' }), { status: 201 })
  })
  const usuario = { nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com', direccion: 'Quilmes', password: '12345678' }
  const agencia = { nombre: 'Viajes', email: 'viajes@example.com' }

  assert.equal((await auth.registrarAgencia({ usuario, agencia })).role, 'AGENTE')
  assert.equal(request.path, '/api/auth/register-agencia')
  assert.equal(request.method, 'POST')
  assert.deepEqual(JSON.parse(request.body), { usuario, agencia })
  assert.equal(request.headers.Authorization, undefined)
})

test('clients are summarized by purchases and total, most recent buyers first', () => {
  const ana = { id: 1, nombre: 'Ana', apellido: 'Pérez', correo: 'ana@example.com' }
  const beto = { id: 2, nombre: 'Beto', apellido: 'Gómez', correo: 'beto@example.com' }
  const venta = (id, cliente, precio, fecha) => ({ id, cliente, precio, fecha })

  const resumen = resumirClientes([
    venta(3, ana, 100, '2027-03-01'), venta(2, beto, 500, '2027-04-01'), venta(1, ana, 300, '2027-02-01'),
  ])

  assert.deepEqual(resumen, [
    { cliente: beto, compras: 1, total: 500, ultimaCompra: '2027-04-01' },
    { cliente: ana, compras: 2, total: 400, ultimaCompra: '2027-03-01' },
  ])
  assert.deepEqual(resumirClientes([]), [])
})

const vuelo = (id, origen, destino, fecha, disponibilidad = 5) => ({ id, origen, destino, fecha, hora: '08:30:00', aerolinea: 'Aerolíneas', disponibilidad })

test('flights are offered inside the chosen dates; return flights are the reverse route on or after the outbound date', () => {
  const ida = vuelo(1, 'Buenos Aires', 'Bariloche', '2027-12-01')
  const rango = { desde: '2027-12-01', hasta: '2027-12-10' }
  const todos = [
    ida, vuelo(2, 'Bariloche', 'Buenos Aires', '2027-12-10'), vuelo(3, 'Bariloche', 'Buenos Aires', '2027-11-20'),
    vuelo(4, 'Bariloche', 'Buenos Aires', '2027-12-09', 0), vuelo(5, 'Bariloche', 'Córdoba', '2027-12-10'),
    vuelo(6, 'Buenos Aires', 'Salta', '2027-12-02', 0), vuelo(7, 'Bariloche', 'Buenos Aires', '2027-12-11'),
  ]

  assert.deepEqual(vuelos.opcionesIda(todos, rango, null).map((v) => v.id), [1, 2, 5])
  assert.deepEqual(vuelos.opcionesVuelta(todos, ida, rango, null).map((v) => v.id), [2])
  assert.deepEqual(vuelos.opcionesVuelta(todos, undefined, rango, null), [])
  // Editing keeps the flights the package already uses, even without seats.
  assert.deepEqual(vuelos.opcionesVuelta(todos, ida, rango, 4).map((v) => v.id), [2, 4])
  assert.deepEqual(vuelos.opcionesIda(todos, rango, 6).map((v) => v.id), [1, 2, 5, 6])
  assert.deepEqual(vuelos.opcionesIda(todos, { desde: '2028-01-01', hasta: '2028-01-05' }, null), [])
})

test('a flight option shows the route, when and with whom it flies, and flags the last seats', () => {
  const opcion = vuelos.opcionVuelo(vuelo(7, 'Buenos Aires', 'Bariloche', '2027-12-01', 1))

  assert.equal(opcion.value, '7')
  assert.equal(opcion.label, 'Buenos Aires → Bariloche')
  assert.match(opcion.detalle, /2027.* · 08:30 · Aerolíneas$/)
  assert.deepEqual(opcion.insignia, { texto: '1 lugar', tono: 'poco' })
  assert.deepEqual(vuelos.opcionVuelo(vuelo(8, 'A', 'B', '2027-12-01', 12)).insignia, { texto: '12 lugares', tono: 'ok' })
})

test('a month grid starts on Monday, pads with neighbouring days and covers every day once', () => {
  const semanas = calendario.semanasDelMes(2027, 10)
  const dias = semanas.flat()

  assert.ok(semanas.every((semana) => semana.length === 7))
  // 1 Nov 2027 is a Monday: no leading padding.
  assert.equal(semanas[0][0].iso, '2027-11-01')
  assert.deepEqual(dias.filter((dia) => dia.enMes).map((dia) => dia.dia), Array.from({ length: 30 }, (_, i) => i + 1))
  // 1 Dec 2027 is a Wednesday, so a month starting midweek is padded with the previous month.
  const diciembre = calendario.semanasDelMes(2027, 11)
  assert.equal(diciembre[0][0].iso, '2027-11-29')
  assert.equal(diciembre[0][2].iso, '2027-12-01')
  assert.equal(diciembre[0][0].enMes, false)
})

test('months wrap across years and ISO dates are built without timezone shifts', () => {
  assert.deepEqual(calendario.mesAnterior({ anio: 2027, mes: 0 }), { anio: 2026, mes: 11 })
  assert.deepEqual(calendario.mesSiguiente({ anio: 2026, mes: 11 }), { anio: 2027, mes: 0 })
  assert.equal(calendario.aIso(2027, 0, 5), '2027-01-05')
  assert.equal(calendario.hoyIso(new Date(2027, 2, 9, 23, 59)), '2027-03-09')
  assert.deepEqual(calendario.mesDe('2027-11-10'), { anio: 2027, mes: 10 })
})

test('picking days starts a range, ends it, and restarts when the day is before the start or the range is complete', () => {
  assert.deepEqual(calendario.elegirDia('', '', '2027-11-10'), { desde: '2027-11-10', hasta: '' })
  assert.deepEqual(calendario.elegirDia('2027-11-10', '', '2027-11-20'), { desde: '2027-11-10', hasta: '2027-11-20' })
  assert.deepEqual(calendario.elegirDia('2027-11-10', '', '2027-11-10'), { desde: '2027-11-10', hasta: '2027-11-10' })
  assert.deepEqual(calendario.elegirDia('2027-11-10', '', '2027-11-05'), { desde: '2027-11-05', hasta: '' })
  assert.deepEqual(calendario.elegirDia('2027-11-10', '2027-11-20', '2027-11-25'), { desde: '2027-11-25', hasta: '' })
})
