package com.aterrizAR.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
abstract class Perfil(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long? = null,
) {
    abstract val roleName: String
}
