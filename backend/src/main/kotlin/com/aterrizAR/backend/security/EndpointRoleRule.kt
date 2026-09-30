package com.aterrizAR.backend.security

import com.aterrizAR.backend.model.Roles
import org.springframework.http.HttpMethod

/** Register one bean per endpoint policy; rules must precede the deny-all fallback. */
class EndpointRoleRule(
    val method: HttpMethod,
    val path: String,
    roles: Set<String>,
) {
    val roles: Set<String> = roles.toSet()

    init {
        require(path.startsWith("/api/")) { "Role rules must target the public /api namespace" }
        require(this.roles.isNotEmpty() && this.roles.all { it in Roles.supported }) {
            "Endpoint rules require at least one supported role"
        }
    }
}
