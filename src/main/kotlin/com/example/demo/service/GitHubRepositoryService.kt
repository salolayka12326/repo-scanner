package com.example.demo.service

import com.example.demo.client.GitHubClient
import com.example.demo.dto.BranchInfo
import com.example.demo.dto.RepositoryResponse
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * Applies the business rules on top of raw GitHub data and maps it to this application's
 * own API contract.
 */
@Service
class GitHubRepositoryService(private val gitHubClient: GitHubClient) {

    /**
     * Returns every non-fork repository owned by [username], each enriched with its branches
     * and the latest commit identifier of every branch.
     */
    fun getNonForkRepositoriesWithBranches(username: String): Flux<RepositoryResponse> =
        gitHubClient.getUserRepositories(username)
            .filter { repo -> !repo.fork }
            .flatMap { repo ->
                gitHubClient.getBranches(repo.owner.login, repo.name)
                    .map { branch -> BranchInfo(name = branch.name, lastCommitSha = branch.commit.sha) }
                    .collectList()
                    .map { branches -> RepositoryResponse(name = repo.name, branches = branches) }
            }
}
