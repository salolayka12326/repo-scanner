package com.example.demo.client

import com.example.demo.client.dto.GitHubBranchDto
import com.example.demo.client.dto.GitHubRepoDto
import com.example.demo.exception.GitHubUpstreamException
import com.example.demo.exception.GitHubUserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Thin wrapper around GitHub's public REST API.
 * Translates GitHub-specific error responses into this application's own exception hierarchy,
 * so upper layers never depend on GitHub's response format or status codes directly.
 */
@Component
class GitHubClient(private val webClient: WebClient) {

    /**
     * Lists all repositories owned by the given user (public repositories only, GitHub's default for this endpoint).
     */
    fun getUserRepositories(username: String): Flux<GitHubRepoDto> =
        webClient.get()
            .uri("/users/{username}/repos?per_page=100&type=owner", username)
            .retrieve()
            .onStatus({ it == HttpStatus.NOT_FOUND }) {
                Mono.error(GitHubUserNotFoundException(username))
            }
            .onStatus({ it.isError }) { response ->
                response.bodyToMono(String::class.java)
                    .defaultIfEmpty("")
                    .flatMap { body ->
                        Mono.error(
                            GitHubUpstreamException(
                                "GitHub API returned an error while listing repositories for '$username': ${response.statusCode()} $body",
                                response.statusCode()
                            )
                        )
                    }
            }
            .bodyToFlux(GitHubRepoDto::class.java)

    /**
     * Lists branches of a repository. The `commit.sha` of each branch already represents
     * the latest commit on that branch, so no further requests are required.
     */
    fun getBranches(owner: String, repo: String): Flux<GitHubBranchDto> =
        webClient.get()
            .uri("/repos/{owner}/{repo}/branches?per_page=100", owner, repo)
            .retrieve()
            .onStatus({ it.isError }) { response ->
                response.bodyToMono(String::class.java)
                    .defaultIfEmpty("")
                    .flatMap { body ->
                        Mono.error(
                            GitHubUpstreamException(
                                "GitHub API returned an error while listing branches for '$owner/$repo': ${response.statusCode()} $body",
                                response.statusCode()
                            )
                        )
                    }
            }
            .bodyToFlux(GitHubBranchDto::class.java)
}
