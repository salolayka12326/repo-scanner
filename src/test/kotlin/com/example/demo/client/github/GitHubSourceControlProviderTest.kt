package com.example.demo.client.github

import com.example.demo.client.github.dto.GitHubBranchDto
import com.example.demo.client.github.dto.GitHubRepoDto
import com.example.demo.exception.RepositoryProviderUnavailableException
import com.example.demo.exception.RepositoryUserNotFoundException
import com.example.demo.client.github.exception.GitHubUpstreamException
import com.example.demo.client.github.exception.GitHubUserNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class GitHubSourceControlProviderTest {

    private val gitHubClient: GitHubClient = mock()
    private val provider = GitHubSourceControlProvider(gitHubClient, GitHubRepositoryMapper())

    @Test
    fun `filters out forks and maps to domain model`() {
        whenever(gitHubClient.getUserRepositories("octocat")).thenReturn(
            Flux.just(
                GitHubRepoDto(name = "owned-repo", fork = false, owner = GitHubRepoDto.Owner("octocat")),
                GitHubRepoDto(name = "forked-repo", fork = true, owner = GitHubRepoDto.Owner("octocat"))
            )
        )

        StepVerifier.create(provider.getNonForkRepositories("octocat"))
            .expectNextMatches { it.name == "owned-repo" && it.owner == "octocat" }
            .verifyComplete()
    }

    @Test
    fun `maps branch dto to domain model`() {
        whenever(gitHubClient.getBranches("octocat", "owned-repo")).thenReturn(
            Flux.just(GitHubBranchDto(name = "main", commit = GitHubBranchDto.CommitRef(sha = "sha-1")))
        )

        StepVerifier.create(provider.getBranches("octocat", "owned-repo"))
            .expectNextMatches { it.name == "main" && it.lastCommitSha == "sha-1" }
            .verifyComplete()
    }

    @Test
    fun `translates client not-found exception into a business exception`() {
        whenever(gitHubClient.getUserRepositories("missing-user"))
            .thenReturn(Flux.error(GitHubUserNotFoundException("missing-user")))

        StepVerifier.create(provider.getNonForkRepositories("missing-user"))
            .expectErrorMatches { it is RepositoryUserNotFoundException }
            .verify()
    }

    @Test
    fun `translates client upstream exception into a business exception`() {
        whenever(gitHubClient.getUserRepositories("octocat"))
            .thenReturn(Flux.error(GitHubUpstreamException("GitHub is down")))

        StepVerifier.create(provider.getNonForkRepositories("octocat"))
            .expectErrorMatches { it is RepositoryProviderUnavailableException }
            .verify()
    }
}
