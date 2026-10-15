package com.aterrizAR.backend.auth

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.server.ResponseStatusException

// Aplica a toda la API: los errores se responden aquí con ApiErrorResponseDTO en vez de reenviarse a /error.
@RestControllerAdvice
class AuthExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponseDTO> {
        val fields = exception.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }
        return error(HttpStatus.BAD_REQUEST, "Validation failed", fields)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(exception: ConstraintViolationException): ResponseEntity<ApiErrorResponseDTO> =
        error(HttpStatus.BAD_REQUEST, "Validation failed")

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidation(exception: HandlerMethodValidationException): ResponseEntity<ApiErrorResponseDTO> =
        error(HttpStatus.BAD_REQUEST, "Validation failed")

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(exception: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponseDTO> =
        error(HttpStatus.BAD_REQUEST, "Malformed or incomplete request body")

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(exception: ResponseStatusException): ResponseEntity<ApiErrorResponseDTO> {
        val status = HttpStatus.resolve(exception.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        return error(status, exception.reason ?: status.reasonPhrase)
    }

    @ExceptionHandler(DuplicateEmailException::class)
    fun handleDuplicateEmail(exception: DuplicateEmailException): ResponseEntity<ApiErrorResponseDTO> =
        error(HttpStatus.CONFLICT, "Email already registered")

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(exception: InvalidCredentialsException): ResponseEntity<ApiErrorResponseDTO> =
        error(HttpStatus.UNAUTHORIZED, "Invalid credentials")

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun handleInvalidRefreshToken(exception: InvalidRefreshTokenException): ResponseEntity<ApiErrorResponseDTO> =
        error(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token")

    private fun error(
        status: HttpStatus,
        message: String,
        fieldErrors: Map<String, String>? = null,
    ): ResponseEntity<ApiErrorResponseDTO> = ResponseEntity.status(status).body(
        ApiErrorResponseDTO(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            fieldErrors = fieldErrors,
        ),
    )
}
