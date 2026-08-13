package com.example.demo.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Partial mapping of the response returned by GitHub's "list repositories for a user" endpoint.
 * Only the fields required by this application are declared; everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubRepoDto(
    val name: String,
    val fork: Boolean,
    val owner: Owner
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Owner(val login: String)
}
