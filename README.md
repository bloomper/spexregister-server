# Spexregister Server 2.0

## Overview

Spexregister Server is the second generation of the backend responsible for serving data related to Spexregister.
It is a Spring Boot application with a REST API, a GraphQL API and an MCP server, backed by MySQL, a Lucene search
index and Keycloak ([fgv-keycloak][8]) for authentication. [Spexregister Web][9] is its frontend and
[spexregister-deploy][10] runs it on a server.

## Learning more

To learn more about Spexregister Server, please consult the [reference documentation][1]. When running locally,
the reference documentation is also available [here][7]. Its source is in `src/main/asciidoc`, and
`getting-started.adoc` describes how to run the whole stack locally with `docker compose`.

## Building from source

You will need Java 25 or later to build Spexregister Server, and Docker for the integration tests.
It is built using [Gradle][2]:

```
./gradlew test              # unit tests
./gradlew integrationTest   # integration tests, against MySQL and Keycloak in Testcontainers
./gradlew build             # both, plus the reference documentation
```

## Published images

CI builds the image with Jib and publishes `bloomper/spexregister-server` for `linux/amd64` and `linux/arm64`:

| Trigger              | Tags                              |
|----------------------|-----------------------------------|
| push to `develop`    | `develop`, `sha-<commit>`         |
| push of tag `vX.Y.Z` | `X.Y.Z`, `latest`, `sha-<commit>` |

To release, set `version` in `build.gradle` to `X.Y.Z`, merge to `main`, then `git tag vX.Y.Z && git push origin vX.Y.Z`.
The build fails if the tag doesn't match the version or if `X.Y.Z` is already on Docker Hub.

## Contributing

Contributors to this project agree to uphold its [code of conduct][3].

You can contribute to Spexregister Server by:

- Opening a [pull request][4]. Please see the [contributor guidelines][5] for details

## License

Spexregister Server is open source software released under the [Apache 2.0 license][6].

[1]: https://register.fgv.nu/docs/index.html
[2]: https://gradle.org
[3]: CODE_OF_CONDUCT.md
[4]: https://help.github.com/articles/using-pull-requests/
[5]: CONTRIBUTING.md
[6]: https://www.apache.org/licenses/LICENSE-2.0.html
[7]: http://localhost:8080/docs/index.html
[8]: https://github.com/bloomper/fgv-keycloak
[9]: https://github.com/bloomper/spexregister-web
[10]: https://github.com/bloomper/spexregister-deploy
