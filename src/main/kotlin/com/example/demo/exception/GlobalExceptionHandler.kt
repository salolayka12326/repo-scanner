package com.example.demo.exception

import com.example.demo.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.NotAcceptableStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.UnsupportedMediaTypeStatusException
import reactor.core.publisher.Mono
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(RepositoryUserNotFoundException::class)
    fun handleUserNotFound(
        ex: RepositoryUserNotFoundException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> =
        buildResponse(HttpStatus.NOT_FOUND, ex.message ?: "Not found", exchange)

    @ExceptionHandler(RepositoryProviderUnavailableException::class)
    fun handleUpstreamFailure(
        ex: RepositoryProviderUnavailableException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> =
        buildResponse(HttpStatus.BAD_GATEWAY, ex.message ?: "Upstream provider error", exchange)

    @ExceptionHandler(NotAcceptableStatusException::class)
    fun handleNotAcceptable(
        ex: NotAcceptableStatusException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> = buildResponse(
        HttpStatus.NOT_ACCEPTABLE,
        "The requested media type is not supported. This API only produces application/json.",
        exchange
    )

    @ExceptionHandler(UnsupportedMediaTypeStatusException::class)
    fun handleUnsupportedMediaType(
        ex: UnsupportedMediaTypeStatusException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> = buildResponse(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "The request body's media type is not supported by this API.",
        exchange
    )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> = buildResponse(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected internal error occurred.",
        exchange
    )

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val body = ErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = exchange.request.path.value(),
            timestamp = Instant.now()
        )
        return Mono.just(ResponseEntity.status(status).body(body))
    }
}
