package com.example.demo.client.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Partial mapping of the response returned by GitHub's "list branches" endpoint.
 * The `commit.sha` field already represents the latest commit on the branch,
 * so no additional call to the commits API is required.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitHubBranchDto(
    val name: String,
    val commit: CommitRef
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CommitRef(val sha: String)
}
