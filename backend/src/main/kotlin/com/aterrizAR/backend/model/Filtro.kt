package com.aterrizAR.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Filtro(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
