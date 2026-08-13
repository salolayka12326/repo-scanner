package com.example.demo.exception

import org.springframework.http.HttpStatusCode

sealed class GitHubApiException(message: String) : RuntimeException(message)

class GitHubUserNotFoundException(username: String) :
    GitHubApiException("GitHub user '$username' was not found")

class GitHubUpstreamException(
    message: String,
    val upstreamStatus: HttpStatusCode? = null
) : GitHubApiException(message)
