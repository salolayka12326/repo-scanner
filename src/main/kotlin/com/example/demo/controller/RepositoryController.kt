package com.example.demo.controller

import com.example.demo.dto.RepositoryResponse
import com.example.demo.service.RepositoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/users/{username}/repositories", produces = [MediaType.APPLICATION_JSON_VALUE])
class RepositoryController(private val repositoryService: RepositoryService) {

    @Operation(
        summary = "List non-fork repositories of a GitHub user",
        description = "Returns every repository owned by the given user that is not a fork, " +
            "together with its branches and the latest commit identifier for each branch."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Repositories retrieved successfully"),
        ApiResponse(responseCode = "404", description = "The GitHub user does not exist"),
        ApiResponse(responseCode = "406", description = "The requested media type is not supported"),
        ApiResponse(responseCode = "502", description = "The GitHub API returned an error"),
        ApiResponse(responseCode = "500", description = "An unexpected internal error occurred")
    )
    @GetMapping
    fun getRepositories(@PathVariable username: String): Mono<List<RepositoryResponse>> =
        repositoryService.getNonForkRepositoriesWithBranches(username).collectList()
}
