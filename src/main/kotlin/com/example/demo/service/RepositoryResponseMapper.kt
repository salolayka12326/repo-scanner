package com.example.demo.service

import com.example.demo.dto.BranchInfo
import com.example.demo.dto.RepositoryResponse
import com.example.demo.provider.model.Branch
import com.example.demo.provider.model.Repository
import org.springframework.stereotype.Component

/** Maps the provider-agnostic domain model to the API response DTOs. */
@Component
class RepositoryResponseMapper {

    fun toResponse(repository: Repository, branches: List<Branch>): RepositoryResponse =
        RepositoryResponse(
            name = repository.name,
            branches = branches.map(::toBranchInfo)
        )

    private fun toBranchInfo(branch: Branch): BranchInfo =
        BranchInfo(name = branch.name, lastCommitSha = branch.lastCommitSha)
}
