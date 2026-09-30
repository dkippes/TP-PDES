package com.aterrizAR.backend.security

import com.aterrizAR.backend.model.Roles
import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('" + Roles.COMPRADOR + "')")
annotation class SoloComprador

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('" + Roles.AGENTE + "')")
annotation class SoloAgente

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('" + Roles.ADMINISTRADOR + "')")
annotation class SoloAdministrador
