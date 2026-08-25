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
    fun handleInvalidRequest(): ResponseEntity<ErrorMessage> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorMessage(INVALID_REQUEST_MESSAGE))

    @ExceptionHandler(EmailAlreadyInUseException::class)
    fun handleEmailAlreadyInUse(exception: EmailAlreadyInUseException): ResponseEntity<ErrorMessage> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorMessage(exception.message ?: EMAIL_ALREADY_IN_USE_MESSAGE))

    @ExceptionHandler(AccessDeniedException::class, AuthenticationException::class)
    fun rethrowSecurityException(exception: Exception): Unit = throw exception

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(exception: Exception): ResponseEntity<ErrorMessage> {
        logger.error("Unhandled exception", exception)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorMessage(INTERNAL_ERROR_MESSAGE))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
        private const val INVALID_REQUEST_MESSAGE = "Invalid request data"
        private const val EMAIL_ALREADY_IN_USE_MESSAGE = "Email already in use"
        private const val INTERNAL_ERROR_MESSAGE = "Internal server error"
    }
}
