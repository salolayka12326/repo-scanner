package com.example.demo.client.github

import com.example.demo.client.github.exception.GitHubUpstreamException
import com.example.demo.client.github.exception.GitHubUserNotFoundException
import com.example.demo.exception.RepositoryProviderUnavailableException
import com.example.demo.exception.RepositoryUserNotFoundException
import com.example.demo.provider.SourceControlProvider
import com.example.demo.provider.model.Branch
import com.example.demo.provider.model.Repository
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * [SourceControlProvider] implementation backed by the GitHub REST API. Adapts the
 * GitHub-specific client, DTOs and exceptions to the provider-agnostic port used by
 * the business layer.
 */
@Component
class GitHubSourceControlProvider(
    private val gitHubClient: GitHubClient,
    private val mapper: GitHubRepositoryMapper
) : SourceControlProvider {

    override fun getNonForkRepositories(username: String): Flux<Repository> =
        gitHubClient.getUserRepositories(username)
            .filter { repo -> !repo.fork }
            .map(mapper::toRepository)
            .onErrorMap(GitHubUserNotFoundException::class.java) { RepositoryUserNotFoundException(username, it) }
            .onErrorMap(GitHubUpstreamException::class.java) { translateUpstreamError(it) }

    override fun getBranches(owner: String, repositoryName: String): Flux<Branch> =
        gitHubClient.getBranches(owner, repositoryName)
            .map(mapper::toBranch)
            .onErrorMap(GitHubUpstreamException::class.java) { translateUpstreamError(it) }

    private fun translateUpstreamError(ex: GitHubUpstreamException): RepositoryProviderUnavailableException =
        RepositoryProviderUnavailableException(ex.message ?: "GitHub API error", ex)
}
