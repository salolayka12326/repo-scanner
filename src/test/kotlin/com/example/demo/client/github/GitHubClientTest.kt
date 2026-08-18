package com.example.demo.client.github

import com.example.demo.client.github.exception.GitHubUpstreamException
import com.example.demo.client.github.exception.GitHubUserNotFoundException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

class GitHubClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: GitHubClient

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        val webClient = WebClient.builder().baseUrl(server.url("/").toString()).build()
        client = GitHubClient(webClient)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `maps 404 to GitHubUserNotFoundException`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not Found"}"""))

        StepVerifier.create(client.getUserRepositories("missing-user"))
            .expectErrorMatches { it is GitHubUserNotFoundException }
            .verify()
    }

    @Test
    fun `maps other error statuses to GitHubUpstreamException`() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"Service Unavailable"}"""))

        StepVerifier.create(client.getUserRepositories("octocat"))
            .expectErrorMatches { it is GitHubUpstreamException }
            .verify()
    }

    @Test
    fun `parses repositories on success`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {"name":"repo-a","fork":false,"owner":{"login":"octocat"}},
                      {"name":"repo-b","fork":true,"owner":{"login":"octocat"}}
                    ]
                    """.trimIndent()
                )
        )

        StepVerifier.create(client.getUserRepositories("octocat"))
            .expectNextMatches { it.name == "repo-a" && !it.fork }
            .expectNextMatches { it.name == "repo-b" && it.fork }
            .verifyComplete()
    }

    @Test
    fun `parses branches on success`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    [
                      {"name":"main","commit":{"sha":"abc123"}}
                    ]
                    """.trimIndent()
                )
        )

        StepVerifier.create(client.getBranches("octocat", "repo-a"))
            .expectNextMatches { it.name == "main" && it.commit.sha == "abc123" }
            .verifyComplete()
    }

    @Test
    fun `follows Link header pagination to collect every page`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("Link", "<${server.url("/users/octocat/repos?page=2")}>; rel=\"next\"")
                .setBody("""[{"name":"repo-page-1","fork":false,"owner":{"login":"octocat"}}]""")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""[{"name":"repo-page-2","fork":false,"owner":{"login":"octocat"}}]""")
        )

        StepVerifier.create(client.getUserRepositories("octocat"))
            .expectNextMatches { it.name == "repo-page-1" }
            .expectNextMatches { it.name == "repo-page-2" }
            .verifyComplete()
    }
}
