package com.example.demo.exception

/**
 * Business-level exceptions raised by the [com.example.demo.provider.SourceControlProvider]
 * abstraction. These are provider-agnostic and are what the controller layer reacts to;
 * component-specific exceptions (e.g. GitHub client errors) are translated into these
 * by the corresponding provider implementation and never leak past it.
 */
sealed class RepositoryProviderException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class RepositoryUserNotFoundException(username: String, cause: Throwable? = null) :
    RepositoryProviderException("User '$username' was not found", cause)

class RepositoryProviderUnavailableException(message: String, cause: Throwable? = null) :
    RepositoryProviderException(message, cause)
