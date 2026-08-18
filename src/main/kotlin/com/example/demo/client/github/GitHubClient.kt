package com.example.demo.client.github

import com.example.demo.client.github.dto.GitHubBranchDto
import com.example.demo.client.github.dto.GitHubRepoDto
import com.example.demo.client.github.exception.GitHubUpstreamException
import com.example.demo.client.github.exception.GitHubUserNotFoundException
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Thin wrapper around the GitHub REST API. Transparently follows pagination (via the
 * `Link` response header, see https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api)
 * so callers always receive the full result set instead of just the first page.
 */
@Component
class GitHubClient(private val webClient: WebClient) {

    fun getUserRepositories(username: String): Flux<GitHubRepoDto> =
        fetchAllPages(
            elementType = REPO_LIST_TYPE,
            errorContext = "listing repositories for '$username'",
            notFound = { GitHubUserNotFoundException(username) }
        ) { page ->
            webClient.get().uri("/users/{username}/repos?per_page=$PAGE_SIZE&type=owner&page={page}", username, page)
        }

    fun getBranches(owner: String, repo: String): Flux<GitHubBranchDto> =
        fetchAllPages(
            elementType = BRANCH_LIST_TYPE,
            errorContext = "listing branches for '$owner/$repo'"
        ) { page ->
            webClient.get().uri("/repos/{owner}/{repo}/branches?per_page=$PAGE_SIZE&page={page}", owner, repo, page)
        }

    private fun <T : Any> fetchAllPages(
        elementType: ParameterizedTypeReference<List<T>>,
        errorContext: String,
        notFound: (() -> Throwable)? = null,
        request: (page: Int) -> WebClient.RequestHeadersSpec<*>
    ): Flux<T> {
        fun fetchPage(page: Int): Mono<Page<T>> =
            request(page).exchangeToMono { response ->
                val status = response.statusCode()
                when {
                    notFound != null && status == HttpStatus.NOT_FOUND ->
                        Mono.error<Page<T>>(notFound())

                    status.isError ->
                        response.bodyToMono(String::class.java)
                            .defaultIfEmpty("")
                            .flatMap<Page<T>> { body ->
                                Mono.error(upstreamError(errorContext, status, body))
                            }

                    else ->
                        response.bodyToMono(elementType)
                            .defaultIfEmpty(emptyList())
                            .map { items -> Page(page, items, hasNextPage(response.headers().asHttpHeaders())) }
                }
            }

        return fetchPage(1)
            .expand { page -> if (page.hasNext) fetchPage(page.number + 1) else Mono.empty() }
            .flatMapIterable { it.items }
    }

    private fun upstreamError(errorContext: String, status: HttpStatusCode, body: String): GitHubUpstreamException =
        GitHubUpstreamException("GitHub API returned an error while $errorContext: $status $body", status)

    private fun hasNextPage(headers: HttpHeaders): Boolean =
        headers.getFirst(HttpHeaders.LINK)?.contains("rel=\"next\"") == true

    private data class Page<T : Any>(val number: Int, val items: List<T>, val hasNext: Boolean)

    companion object {
        private const val PAGE_SIZE = 100
        private val REPO_LIST_TYPE = object : ParameterizedTypeReference<List<GitHubRepoDto>>() {}
        private val BRANCH_LIST_TYPE = object : ParameterizedTypeReference<List<GitHubBranchDto>>() {}
    }
}
