package com.example.demo.service

import com.example.demo.provider.SourceControlProvider
import com.example.demo.provider.model.Branch
import com.example.demo.provider.model.Repository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

class RepositoryServiceTest {

    private val sourceControlProvider: SourceControlProvider = mock()
    private val service = RepositoryService(sourceControlProvider, RepositoryResponseMapper())

    @Test
    fun `returns repositories with their branches`() {
        val repository = Repository(name = "owned-repo", owner = "octocat")

        whenever(sourceControlProvider.getNonForkRepositories("octocat"))
            .thenReturn(Flux.just(repository))
        whenever(sourceControlProvider.getBranches("octocat", "owned-repo"))
            .thenReturn(
                Flux.just(
                    Branch(name = "main", lastCommitSha = "sha-main"),
                    Branch(name = "dev", lastCommitSha = "sha-dev")
                )
            )

        StepVerifier.create(service.getNonForkRepositoriesWithBranches("octocat"))
            .assertNext { response ->
                assert(response.name == "owned-repo")
                assert(response.branches.map { it.name } == listOf("main", "dev"))
                assert(response.branches.map { it.lastCommitSha } == listOf("sha-main", "sha-dev"))
            }
            .verifyComplete()
    }

    @Test
    fun `returns empty result when user has no repositories`() {
        whenever(sourceControlProvider.getNonForkRepositories("empty-user")).thenReturn(Flux.empty())

        StepVerifier.create(service.getNonForkRepositoriesWithBranches("empty-user"))
            .verifyComplete()
    }

    @Test
    fun `propagates errors raised by the source control provider`() {
        val error = RuntimeException("boom")
        whenever(sourceControlProvider.getNonForkRepositories("octocat")).thenReturn(Flux.error(error))

        StepVerifier.create(service.getNonForkRepositoriesWithBranches("octocat"))
            .expectErrorMatches { it === error }
            .verify()
    }
}
