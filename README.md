# GitHub Repositories API

A Kotlin + Spring Boot WebFlux service that exposes non-fork GitHub repositories
of a given user, together with their branches and the latest commit identifier
of each branch. The API contract is independent of GitHub's own response format.

## How to build and run

Requirements: JDK 21.

Build and run tests:

```powershell
.\gradlew.bat build
```

Run the application:

```powershell
.\gradlew.bat bootRun
```

The application starts on `http://localhost:8080`.

Example request:

```powershell
curl http://localhost:8080/api/users/octocat/repositories
```

### Optional configuration

`src/main/resources/application.properties`:

- `github.api.base-url` — base URL of the GitHub REST API (default `https://api.github.com`).
- `github.api.token` — optional GitHub personal access token. GitHub's unauthenticated
  rate limit is low (60 requests/hour); setting a token raises it to 5000 requests/hour.
  Leave empty to call the API anonymously.

## API documentation

The OpenAPI specification is served by the application itself:

- Spec: `http://localhost:8080/openapi.yaml`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

The spec is also available as a static file at
[src/main/resources/static/openapi.yaml](src/main/resources/static/openapi.yaml).

Note: `springdoc-openapi` (auto-generated OpenAPI from annotations) was evaluated but
is currently incompatible with Spring Boot 4 / Spring Framework 7 (`NoSuchMethodError`
on `UriComponentsBuilder`). The specification is therefore maintained as a static file
and served via `org.webjars:swagger-ui`, while `@Schema`/`@Operation` annotations are
kept on the DTOs/controller for documentation purposes and to make a future migration
back to `springdoc` straightforward.

## Design decisions

### Layering

```
controller  -> service -> client -> GitHub REST API
   |              |          |
 RepositoryResponse   GitHubRepoDto / GitHubBranchDto (raw GitHub shapes)
```

- **`client` (`GitHubClient`)** — the only layer that knows about GitHub's HTTP API,
  status codes and response shapes. It translates GitHub errors into this
  application's own exception hierarchy (`GitHubUserNotFoundException`,
  `GitHubUpstreamException`), so no other layer depends on GitHub's status codes.
- **`service` (`GitHubRepositoryService`)** — business rules (exclude forks) and
  mapping from GitHub's raw DTOs to the public API contract.
- **`controller` (`RepositoryController`)** — thin HTTP layer; only wires the
  request to the service and lets Spring/the global exception handler take care
  of status codes and content negotiation.
- **`exception`** — a single `GlobalExceptionHandler` (`@RestControllerAdvice`)
  guarantees a consistent `ErrorResponse` body across all error paths.

This separation keeps GitHub-specific concerns fully isolated in one class,
which makes the service and controller trivially testable without any HTTP
mocking, and makes it possible to swap the data source without touching the
public API contract.

### Why WebClient over RestClient/RestTemplate

`RestTemplate` is blocking and does not fit a WebFlux application. `WebClient` is
used for both calls to GitHub (list repositories, list branches per repository),
issued concurrently via `Flux.flatMap`, so branch lookups for multiple
repositories happen in parallel rather than sequentially.

### Avoiding an extra "get latest commit" call

GitHub's `GET /repos/{owner}/{repo}/branches` response already includes
`commit.sha` for each branch, which is the latest commit on that branch. Using
this field avoids an additional `GET /repos/{owner}/{repo}/commits/{branch}`
call per branch, keeping the number of GitHub requests to `1 + N` (one call to
list repositories, one call per non-fork repository to list its branches).

### Error handling and HTTP status codes

| Situation | Status | Rationale |
|---|---|---|
| GitHub user does not exist | `404 Not Found` | GitHub itself returns 404 for the user; surfacing the same semantics to our own client is the most intuitive mapping. |
| Client requests a representation we don't produce (e.g. `Accept: application/xml`) | `406 Not Acceptable` | Standard HTTP semantics for content negotiation failures; the API only produces JSON. |
| Client sends a request body with an unsupported `Content-Type` | `415 Unsupported Media Type` | Standard HTTP semantics; kept for completeness even though the current API has no endpoints accepting a body. |
| GitHub API fails (rate limiting, 5xx, network error) | `502 Bad Gateway` | The failure originates in an upstream dependency, not in this service; `502` communicates that the fault is not this API's own bug. |
| Any other unhandled exception | `500 Internal Server Error` | Generic fallback so the client never sees a stack trace or a non-JSON error page. |

Every error path returns the same JSON shape (`ErrorResponse`): `status`,
`error`, `message`, `path`, `timestamp`. This lets clients handle all failures
generically while still branching on `status` for specific cases.

### API contract

`GET /api/users/{username}/repositories` returns a JSON array of:

```json
{
  "name": "hello-world",
  "branches": [
    { "name": "main", "lastCommitSha": "553c2077f0edc3d5dc5d17262f6aa498e69d6f8e" }
  ]
}
```

This shape is deliberately independent from GitHub's response format (which
includes dozens of fields per repository/branch that consumers of this API do
not need).

## Assumptions

- "Repositories belonging to a user" means repositories owned by that user
  (`GET /users/{username}/repos`), not repositories they merely collaborate on.
- Only public repositories are considered, since the API is called
  unauthenticated by default (or with a personal token that has no special
  private-repo access configured).
- "Latest commit identifier for each branch" is satisfied by the branch's head
  commit SHA as reported by GitHub's branches endpoint.
- A GitHub organization name can be passed as `username` as well, since GitHub's
  `/users/{username}/repos` endpoint also resolves organizations.

## Limitations

- Pagination is not implemented: requests use `per_page=100`, so users/orgs with
  more than 100 repositories, or repositories with more than 100 branches, will
  only have their first 100 items returned.
- No caching layer: every request re-fetches data from GitHub. For a user with
  many repositories this means many concurrent outbound requests per inbound
  request, which is bounded only by GitHub's own rate limits.
- No retry/backoff around GitHub calls; a transient GitHub failure surfaces
  immediately as `502` rather than being retried.
- Unauthenticated GitHub API calls are limited to 60 requests/hour; set
  `github.api.token` to raise this limit for real usage.
