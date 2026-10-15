package com.aterrizAR.backend.usuario

import com.aterrizAR.backend.agencia.AgenciaService
import com.aterrizAR.backend.auth.RefreshTokenService
import com.aterrizAR.backend.model.Administrador
import com.aterrizAR.backend.model.Agente
import com.aterrizAR.backend.model.Comprador
import com.aterrizAR.backend.model.Perfil
import com.aterrizAR.backend.model.Roles
import com.aterrizAR.backend.model.Usuario
import com.aterrizAR.backend.security.UsuarioAutenticado
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class UsuarioService(
    private val usuarioRepository: UsuarioRepository,
    private val agenciaService: AgenciaService,
    private val refreshTokenService: RefreshTokenService,
    private val passwordEncoder: PasswordEncoder,
) {
    fun findAll(rol: String?): List<Usuario> =
        if (rol == null) usuarioRepository.findAllConPerfil()
        else usuarioRepository.findAllByTipoPerfil(tipoPerfil(rol))

    fun findById(id: Long): Usuario = usuarioRepository.findById(id)
        .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario $id no encontrado") }

    @Transactional
    fun create(request: UsuarioCreateRequestDTO): Usuario {
        val correo = normalizarCorreo(request.correo)
        validateCorreoUnico(correo, excludeId = null)
        val usuario = Usuario(
            nombre = request.nombre.trim(),
            apellido = request.apellido.trim(),
            correo = correo,
            direccion = request.direccion.trim(),
            passwordHash = hash(request.password),
            perfil = perfilPara(request.rol, request.agenciaId),
        )
        return save(usuario)
    }

    @Transactional
    fun update(id: Long, request: UsuarioUpdateRequestDTO, autenticado: UsuarioAutenticado): Usuario {
        val usuario = findById(id)
        if (id == autenticado.id && request.rol != Roles.ADMINISTRADOR) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No puede quitarse el rol de administrador a sí mismo")
        }
        val correo = normalizarCorreo(request.correo)
        // Antes de modificar la entidad: la consulta dispararía un flush de los cambios pendientes.
        validateCorreoUnico(correo, excludeId = id)
        val cambiaRol = usuario.perfil.roleName != request.rol
        val nuevoPerfil = if (cambiaRol || cambiaAgencia(usuario.perfil, request.agenciaId)) {
            perfilPara(request.rol, request.agenciaId)
        } else {
            validateAgenciaId(request.rol, request.agenciaId)
            null
        }

        usuario.nombre = request.nombre.trim()
        usuario.apellido = request.apellido.trim()
        usuario.correo = correo
        usuario.direccion = request.direccion.trim()
        request.password?.let { usuario.passwordHash = hash(it) }
        nuevoPerfil?.let { usuario.perfil = it }
        val saved = save(usuario)

        // El rol viaja en el JWT y la contraseña cambió: se cierran las sesiones renovables del usuario.
        if (cambiaRol || request.password != null) {
            refreshTokenService.revokeAllFor(id)
        }
        return saved
    }

    @Transactional
    fun delete(id: Long, autenticado: UsuarioAutenticado) {
        if (id == autenticado.id) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "No puede eliminar su propio usuario")
        }
        val usuario = findById(id)
        refreshTokenService.deleteAllFor(id)
        try {
            usuarioRepository.delete(usuario)
            usuarioRepository.flush()
        } catch (exception: DataIntegrityViolationException) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT, "El usuario $id tiene compras o valoraciones que impiden eliminarlo", exception,
            )
        }
    }

    fun findActual(autenticado: UsuarioAutenticado): Usuario = findById(autenticado.id)

    @Transactional
    fun actualizarDatosPersonales(autenticado: UsuarioAutenticado, request: DatosPersonalesRequestDTO): Usuario {
        val usuario = findById(autenticado.id)
        usuario.nombre = request.nombre.trim()
        usuario.apellido = request.apellido.trim()
        usuario.direccion = request.direccion.trim()
        return usuarioRepository.saveAndFlush(usuario)
    }

    @Transactional
    fun cambiarPassword(autenticado: UsuarioAutenticado, request: CambioPasswordRequestDTO) {
        val usuario = findById(autenticado.id)

        if (!passwordEncoder.matches(request.passwordActual, usuario.passwordHash)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta")
        }
        usuario.passwordHash = hash(request.passwordNueva)
        usuarioRepository.saveAndFlush(usuario)
        refreshTokenService.revokeAllFor(autenticado.id)
    }

    private fun tipoPerfil(rol: String): Class<out Perfil> = when (rol) {
        Roles.ADMINISTRADOR -> Administrador::class.java
        Roles.AGENTE -> Agente::class.java
        Roles.COMPRADOR -> Comprador::class.java
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol inválido: $rol")
    }

    private fun perfilPara(rol: String, agenciaId: Long?): Perfil {
        validateAgenciaId(rol, agenciaId)
        return when (rol) {
            Roles.ADMINISTRADOR -> Administrador()
            Roles.COMPRADOR -> Comprador()
            Roles.AGENTE -> Agente(agenciaService.findById(requireNotNull(agenciaId)))
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol inválido: $rol")
        }
    }

    private fun validateAgenciaId(rol: String, agenciaId: Long?) {
        if (rol == Roles.AGENTE && agenciaId == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "agenciaId es obligatorio para el rol AGENTE")
        }
        if (rol != Roles.AGENTE && agenciaId != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "agenciaId solo aplica al rol AGENTE")
        }
    }

    private fun cambiaAgencia(perfil: Perfil, agenciaId: Long?): Boolean =
        perfil is Agente && agenciaId != null && perfil.agencia.id != agenciaId

    private fun validateCorreoUnico(correo: String, excludeId: Long?) {
        if (usuarioRepository.existsByCorreoAndIdNot(correo, excludeId ?: -1L)) {
            throw correoDuplicado()
        }
    }

    private fun hash(password: String): String = requireNotNull(passwordEncoder.encode(password))

    // El UNIQUE de la base cubre la carrera con la validación previa; el reemplazo de perfil
    // también falla aquí si el perfil anterior (un Comprador con compras o valoraciones) tiene referencias.
    private fun save(usuario: Usuario): Usuario = try {
        usuarioRepository.saveAndFlush(usuario)
    } catch (exception: DataIntegrityViolationException) {
        throw ResponseStatusException(
            HttpStatus.CONFLICT, "El correo ya está registrado o el perfil anterior tiene compras o valoraciones", exception,
        )
    }

    private fun correoDuplicado() = ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo")
}
