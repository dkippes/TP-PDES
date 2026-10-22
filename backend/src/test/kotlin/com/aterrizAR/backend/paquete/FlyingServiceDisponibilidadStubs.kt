package com.aterrizAR.backend.paquete

import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

private const val FLYING_SERVICE_URL = "http://flying-service:8081"

private fun disponibilidadesUrl(vueloIds: Collection<Long>) =
    "$FLYING_SERVICE_URL/vuelos?ids=${vueloIds.distinct().sorted().joinToString(",")}"

/** Expects the single batched availability read of [PaqueteDisponibilidadService]; flights not listed are "missing". */
fun MockRestServiceServer.expectDisponibilidades(vararg vuelos: Pair<Long, Int>, consultados: Collection<Long> = vuelos.map { it.first }) {
    val cuerpo = vuelos.joinToString(",", "[", "]") { (id, disponibilidad) -> """{"id":$id,"disponibilidad":$disponibilidad}""" }
    expect(requestTo(disponibilidadesUrl(consultados))).andRespond(withSuccess(cuerpo, MediaType.APPLICATION_JSON))
}

/** Expects the batched availability read for [vueloIds] and makes flying-service fail it. */
fun MockRestServiceServer.expectDisponibilidadesFallidas(vararg vueloIds: Long) {
    expect(requestTo(disponibilidadesUrl(vueloIds.toList()))).andRespond(withServerError())
}
