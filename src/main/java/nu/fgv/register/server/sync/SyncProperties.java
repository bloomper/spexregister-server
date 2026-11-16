/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where Spexipedia is, and whether to sync from it ({@code fgv-spexregister.spexipedia.*}).
 *
 * @param enabled    whether to pull Spexipedia's change feed
 * @param graphqlUrl Spexipedia's GraphQL API, e.g. {@code http://spexipedia-api:8080/api/graphql}
 * @param webUrl     Spexipedia's web, for links to its pages
 * @author Anders Jacobsson
 * @since 2.0
 */
@ConfigurationProperties(prefix = "fgv-spexregister.spexipedia")
public record SyncProperties(boolean enabled, String graphqlUrl, String webUrl) {
}
