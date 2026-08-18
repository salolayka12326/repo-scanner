package com.example.demo.client.github

import com.example.demo.client.github.dto.GitHubBranchDto
import com.example.demo.client.github.dto.GitHubRepoDto
import com.example.demo.provider.model.Branch
import com.example.demo.provider.model.Repository
import org.springframework.stereotype.Component

/** Translates GitHub-specific DTOs into the provider-agnostic domain model. */
@Component
class GitHubRepositoryMapper {

    fun toRepository(dto: GitHubRepoDto): Repository =
        Repository(name = dto.name, owner = dto.owner.login)

    fun toBranch(dto: GitHubBranchDto): Branch =
        Branch(name = dto.name, lastCommitSha = dto.commit.sha)
}
