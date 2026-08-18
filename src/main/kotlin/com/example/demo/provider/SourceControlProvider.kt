package com.example.demo.provider

import com.example.demo.provider.model.Branch
import com.example.demo.provider.model.Repository
import reactor.core.publisher.Flux

/**
 * Abstraction over a source control hosting platform (GitHub, GitLab, SVN, ...).
 *
 * The business layer depends only on this port, never on a concrete client. Adding
 * support for another platform means adding a new implementation of this interface,
 * with no changes required to [com.example.demo.service.RepositoryService] or the controller.
 */
interface SourceControlProvider {

    /** Repositories owned by [username], excluding forks. */
    fun getNonForkRepositories(username: String): Flux<Repository>

    /** Branches of the given repository. */
    fun getBranches(owner: String, repositoryName: String): Flux<Branch>
}
