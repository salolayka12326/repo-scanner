package com.example.demo.service

import com.example.demo.dto.RepositoryResponse
import com.example.demo.provider.SourceControlProvider
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

/**
 * Orchestrates repository and branch retrieval through the [SourceControlProvider] abstraction.
 * Depends only on the port, not on any concrete client, and delegates DTO mapping to
 * [RepositoryResponseMapper] so it is free of both provider-specific and mapping concerns.
 */
@Service
class RepositoryService(
    private val sourceControlProvider: SourceControlProvider,
    private val responseMapper: RepositoryResponseMapper
) {

    fun getNonForkRepositoriesWithBranches(username: String): Flux<RepositoryResponse> =
        sourceControlProvider.getNonForkRepositories(username)
            .flatMap { repository ->
                sourceControlProvider.getBranches(repository.owner, repository.name)
                    .collectList()
                    .map { branches -> responseMapper.toResponse(repository, branches) }
            }
}
