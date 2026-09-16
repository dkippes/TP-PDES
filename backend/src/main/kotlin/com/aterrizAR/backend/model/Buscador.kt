package com.aterrizAR.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany

@Entity
class Buscador(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToMany
    @JoinTable(
        name = "buscador_filtro",
        joinColumns = [JoinColumn(name = "buscador_id")],
        inverseJoinColumns = [JoinColumn(name = "filtro_id")],
    )
    val filtros: MutableList<Filtro> = mutableListOf(),
)
