package com.example.demo.client.github.exception

import org.springframework.http.HttpStatusCode

/**
 * Low-level, component-specific exceptions raised by [com.example.demo.client.github.GitHubClient]
 * while talking to the GitHub REST API. These are kept close to the client (and never exposed
 * to controllers directly) so the client package remains self-contained and reusable on its own.
 */
sealed class GitHubClientException(message: String) : RuntimeException(message)

class GitHubUserNotFoundException(username: String) :
    GitHubClientException("GitHub user '$username' was not found")

class GitHubUpstreamException(
    message: String,
    val upstreamStatus: HttpStatusCode? = null
) : GitHubClientException(message)
