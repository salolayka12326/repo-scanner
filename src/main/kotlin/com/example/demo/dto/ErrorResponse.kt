package com.example.demo.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Uniform error payload returned for every failed request")
data class ErrorResponse(
    @Schema(description = "HTTP status code", example = "404")
    val status: Int,

    @Schema(description = "Short, human readable error category", example = "Not Found")
    val error: String,

    @Schema(description = "Human readable explanation of what went wrong")
    val message: String,

    @Schema(description = "Path of the request that failed", example = "/api/users/octocat/repositories")
    val path: String,

    @Schema(description = "Time at which the error occurred")
    val timestamp: Instant = Instant.now()
)
