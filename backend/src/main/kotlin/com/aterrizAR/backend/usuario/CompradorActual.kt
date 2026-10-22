package com.aterrizAR.backend.usuario

import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Usuario
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Buyer profile of the authenticated account. The account may have been deleted or changed role after its token was issued.
 */
fun Usuario?.comoComprador(operacion: String): Comprador {
    if (this == null) throw ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario ya no existe")
    return perfil as? Comprador
        ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los compradores pueden $operacion")
}
