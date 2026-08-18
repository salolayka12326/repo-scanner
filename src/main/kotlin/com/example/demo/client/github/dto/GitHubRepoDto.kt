package com.example.demo.client.github.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubRepoDto(
    val name: String,
    val fork: Boolean,
    val owner: Owner
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Owner(val login: String)
}
