package com.example.demo.controller

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Exercises the full stack (controller -> service -> provider -> GitHub client) over real HTTP,
 * stubbing only the GitHub API itself via [MockWebServer]. No Spring bean is mocked, so the
 * application context stays identical - and cached - across every test in the suite, unlike the
 * previous approach which used `@MockitoBean` and forced a fresh context per test class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RepositoryControllerIntegrationTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `returns non-fork repositories with branches as JSON`() {
        gitHubServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""[{"name":"hello-world","fork":false,"owner":{"login":"octocat"}}]""")
        )
        gitHubServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""[{"name":"main","commit":{"sha":"sha-1"}}]""")
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
        gitHubServer.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not Found"}"""))

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
        gitHubServer.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"Service Unavailable"}"""))

        webTestClient.get()
            .uri("/api/users/octocat/repositories")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
            .expectBody()
            .jsonPath("$.status").isEqualTo(502)
    }

    @Test
    fun `returns 406 when client requests an unsupported media type`() {
        // Rejected by content negotiation before the controller runs, so GitHub is never called.
        webTestClient.get()
            .uri("/api/users/octocat/repositories")
            .accept(MediaType.APPLICATION_XML)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_ACCEPTABLE)
    }

    companion object {
        // Started eagerly (not in @BeforeAll) because @DynamicPropertySource is evaluated
        // while the Spring context is being prepared, which happens before @BeforeAll runs.
        private val gitHubServer = MockWebServer().apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun githubProperties(registry: DynamicPropertyRegistry) {
            registry.add("github.api.base-url") { gitHubServer.url("/").toString() }
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            gitHubServer.shutdown()
        }
    }
}
