package edu.austral.splitit.server.infrastructure.api

import edu.austral.splitit.server.application.exception.EmailAlreadyInUseException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(
        MethodArgumentNotValidException::class,
        HandlerMethodValidationException::class,
        HttpMessageNotReadableException::class,
    )
    fun handleInvalidRequest(exception: Exception): ResponseEntity<ErrorMessage> {
        logger.warn("Invalid request: {}", exception.message)
        return error(HttpStatus.BAD_REQUEST, INVALID_REQUEST_MESSAGE)
    }

    @ExceptionHandler(EmailAlreadyInUseException::class)
    fun handleEmailAlreadyInUse(exception: EmailAlreadyInUseException): ResponseEntity<ErrorMessage> =
        error(HttpStatus.CONFLICT, exception.message ?: EMAIL_ALREADY_IN_USE_MESSAGE)

    @ExceptionHandler(AccessDeniedException::class)
    fun rethrowAccessDenied(exception: AccessDeniedException): Unit = throw exception

    @ExceptionHandler(AuthenticationException::class)
    fun rethrowAuthentication(exception: AuthenticationException): Unit = throw exception

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ErrorMessage> {
        logger.error("Unhandled exception", exception)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE)
    }

    private fun error(
        status: HttpStatus,
        message: String,
    ): ResponseEntity<ErrorMessage> =
        ResponseEntity
            .status(status)
            .body(ErrorMessage(message))

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
        private const val INVALID_REQUEST_MESSAGE = "Invalid request data"
        private const val EMAIL_ALREADY_IN_USE_MESSAGE = "Email already in use"
        private const val INTERNAL_ERROR_MESSAGE = "Internal server error"
    }
}
