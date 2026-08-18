package com.example.demo.client.github.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubBranchDto(
    val name: String,
    val commit: CommitRef
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CommitRef(val sha: String)
}
