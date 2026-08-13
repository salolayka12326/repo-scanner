package com.example.demo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Value("\${github.api.base-url}")
    private lateinit var baseUrl: String

    @Value("\${github.api.token:}")
    private lateinit var token: String

    @Bean
    fun gitHubWebClient(): WebClient {
        val configured = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .defaultHeader(HttpHeaders.USER_AGENT, "demo-github-repositories-app")

        if (token.isNotBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }

        return configured.build()
    }
}
