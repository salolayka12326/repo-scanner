package com.example.demo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A non-fork GitHub repository together with its branches")
data class RepositoryResponse(
    @Schema(description = "Repository name", example = "spring-framework")
    val name: String,

    @Schema(description = "Branches that exist in the repository")
    val branches: List<BranchInfo>
)

@Schema(description = "A single branch and the identifier of its latest commit")
data class BranchInfo(
    @Schema(description = "Branch name", example = "main")
    val name: String,

    @Schema(description = "SHA-1 identifier of the latest commit on this branch", example = "7fd1a60b01f91b314f59955a4e4d4e80d8edf11")
    val lastCommitSha: String
)
