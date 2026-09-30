package com.aterrizAR.backend.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Modifying
    @Query("UPDATE RefreshToken token SET token.revokedAt = :revokedAt WHERE token.familyId = :familyId AND token.revokedAt IS NULL")
    fun revokeActiveByFamilyId(@Param("familyId") familyId: UUID, @Param("revokedAt") revokedAt: Instant): Int
}
