# Spexregister Server 2.0

## Overview

Spexregister Server is the second generation of the backend responsible for serving data related to Spexregister.
It is a Spring Boot application with a REST API, a GraphQL API and an MCP server, backed by MySQL, a Lucene search
index and Keycloak ([fgv-keycloak][8]) for authentication. [Spexregister Web][9] is its frontend and
[spexregister-deploy][10] runs it on a server.

## Roles and access

Roles are client roles on `spexregister` in Keycloak:

| Role      | Who                                  | What                                                       |
|-----------|--------------------------------------|------------------------------------------------------------|
| `USER`    | Every user                           | Reads the register; changes their own and their partner's record |
| `EDITOR`  | Editors                              | Also changes it                                            |
| `ADMIN`   | Administrators                       | Everything, including users, settings and permission grants |
| `REVIVAL` | Nyuppsättning's service account only | Reads spex, tasks and task categories, nothing else      |

Which records a caller may read, write, delete or administer is decided by one access policy per kind of record, in
`nu.fgv.register.server.security.policy`. A policy answers from the caller's roles, the record's own data (published
or not, whose record it is) and any permission grants that cover it. Lists and searches get the same rules as a
query predicate, so a record the caller may not read behaves as if it did not exist. To change who may do what,
change the policy; no stored data has to change.

Admins can grant a user or a role extra rights on one spexare, on everyone with an activity in a spex, or on one spex,
optionally until a date (`permissionGrant*` in GraphQL, *Permissions* under admin in the web). Grants only add rights,
and they are in the change log.

`REVIVAL` is checked with `@RequiresReferenceDataReader` on the operations it may call. It is not an `authority`, so
the user sync never imports the service account as a user. Its tokens must come from an authorized party: set
`SPEXREGISTER_SECURITY_AUTHORIZED_PARTIES` to include `fgv-revival-service`.

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

[1]: https://register.fgv.nu/docs/
[2]: https://gradle.org
[3]: CODE_OF_CONDUCT.md
[4]: https://help.github.com/articles/using-pull-requests/
[5]: CONTRIBUTING.md
[6]: https://www.apache.org/licenses/LICENSE-2.0.html
[7]: http://localhost:8080/docs/
[8]: https://github.com/bloomper/fgv-keycloak
[9]: https://github.com/bloomper/spexregister-web
[10]: https://github.com/bloomper/spexregister-deploy
