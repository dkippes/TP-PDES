package com.aterrizAR.backend.auth

import com.aterrizAR.backend.model.Usuario
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_token")
class RefreshToken(
    @Id
    @GeneratedValue
    val id: UUID? = null,
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    val tokenHash: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    val usuario: Usuario,
    @Column(name = "family_id", nullable = false)
    val familyId: UUID,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
) {
    fun revoke(at: Instant) {
        if (revokedAt == null) {
            revokedAt = at
        }
    }

    fun isActive(at: Instant): Boolean = revokedAt == null && expiresAt.isAfter(at)
}
