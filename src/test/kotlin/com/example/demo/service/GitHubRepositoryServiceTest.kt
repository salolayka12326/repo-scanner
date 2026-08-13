package com.example.demo.service

import com.example.demo.client.GitHubClient
import com.example.demo.client.dto.GitHubBranchDto
import com.example.demo.client.dto.GitHubRepoDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class GitHubRepositoryServiceTest {

    private val gitHubClient: GitHubClient = mock()
    private val service = GitHubRepositoryService(gitHubClient)

    @Test
    fun `returns only non-fork repositories with their branches`() {
        val ownedRepo = GitHubRepoDto(name = "owned-repo", fork = false, owner = GitHubRepoDto.Owner("octocat"))
        val forkedRepo = GitHubRepoDto(name = "forked-repo", fork = true, owner = GitHubRepoDto.Owner("octocat"))

        whenever(gitHubClient.getUserRepositories("octocat"))
            .thenReturn(Flux.just(ownedRepo, forkedRepo))
        whenever(gitHubClient.getBranches("octocat", "owned-repo"))
            .thenReturn(
                Flux.just(
                    GitHubBranchDto(name = "main", commit = GitHubBranchDto.CommitRef(sha = "sha-main")),
                    GitHubBranchDto(name = "dev", commit = GitHubBranchDto.CommitRef(sha = "sha-dev"))
                )
            )

        StepVerifier.create(service.getNonForkRepositoriesWithBranches("octocat"))
            .assertNext { repository ->
                assert(repository.name == "owned-repo")
                assert(repository.branches.map { it.name } == listOf("main", "dev"))
                assert(repository.branches.map { it.lastCommitSha } == listOf("sha-main", "sha-dev"))
            }
            .verifyComplete()
    }

    @Test
    fun `returns empty result when user has no repositories`() {
        whenever(gitHubClient.getUserRepositories("empty-user")).thenReturn(Flux.empty())

        StepVerifier.create(service.getNonForkRepositoriesWithBranches("empty-user"))
            .verifyComplete()
    }

    @Test
    fun `propagates errors raised by the GitHub client`() {
        val error = RuntimeException("boom")
        whenever(gitHubClient.getUserRepositories("octocat")).thenReturn(Flux.error(error))

        StepVerifier.create(service.getNonForkRepositoriesWithBranches("octocat"))
            .expectErrorMatches { it === error }
            .verify()
    }
}
