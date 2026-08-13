package com.example.demo.controller

import com.example.demo.client.GitHubClient
import com.example.demo.client.dto.GitHubBranchDto
import com.example.demo.client.dto.GitHubRepoDto
import com.example.demo.exception.GitHubUpstreamException
import com.example.demo.exception.GitHubUserNotFoundException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RepositoryControllerIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var gitHubClient: GitHubClient

    @Test
    fun `returns non-fork repositories with branches as JSON`() {
        whenever(gitHubClient.getUserRepositories("octocat")).thenReturn(
            Flux.just(GitHubRepoDto(name = "hello-world", fork = false, owner = GitHubRepoDto.Owner("octocat")))
        )
        whenever(gitHubClient.getBranches("octocat", "hello-world")).thenReturn(
            Flux.just(GitHubBranchDto(name = "main", commit = GitHubBranchDto.CommitRef(sha = "sha-1")))
        )

        webTestClient.get()
            .uri("/api/users/octocat/repositories")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].name").isEqualTo("hello-world")
            .jsonPath("$[0].branches[0].name").isEqualTo("main")
            .jsonPath("$[0].branches[0].lastCommitSha").isEqualTo("sha-1")
    }

    @Test
    fun `returns 404 with consistent error body when GitHub user does not exist`() {
        whenever(gitHubClient.getUserRepositories("missing-user"))
            .thenReturn(Flux.error(GitHubUserNotFoundException("missing-user")))

        webTestClient.get()
            .uri("/api/users/missing-user/repositories")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.error").isEqualTo("Not Found")
            .jsonPath("$.path").isEqualTo("/api/users/missing-user/repositories")
    }

    @Test
    fun `returns 502 when GitHub API fails`() {
        whenever(gitHubClient.getUserRepositories("octocat"))
            .thenReturn(Flux.error(GitHubUpstreamException("GitHub is down")))

        webTestClient.get()
            .uri("/api/users/octocat/repositories")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
            .expectBody()
            .jsonPath("$.status").isEqualTo(502)
    }

    @Test
    fun `returns 406 when client requests an unsupported media type`() {
        whenever(gitHubClient.getUserRepositories("octocat")).thenReturn(Flux.empty())

        webTestClient.get()
            .uri("/api/users/octocat/repositories")
            .accept(MediaType.APPLICATION_XML)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_ACCEPTABLE)
    }
}
