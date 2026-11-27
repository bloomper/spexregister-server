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
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Where Medlemskap is, and whether to sync from it ({@code spexregister.medlemskap.*}).
 *
 * @param enabled       whether to pull Medlemskap's membership feed and tell it about deleted spexare
 * @param graphqlUrl    Medlemskap's GraphQL API, e.g. {@code http://membership-api:8080/api/graphql}
 * @param apiUrl        Medlemskap's API root, where deleted spexare are reported
 * @param webUrl        Medlemskap's web, for links to its pages
 * @param internalTypes the membership types that are Medlemskap's, written only by the sync; others are external and
 *                      edited here
 * @author Anders Jacobsson
 * @since 2.0
 */
@ConfigurationProperties(prefix = "spexregister.medlemskap")
public record MedlemskapProperties(boolean enabled, String graphqlUrl, String apiUrl, String webUrl,
                                   @DefaultValue("FGV") List<String> internalTypes) {

    public boolean isInternal(final String typeId) {
        return internalTypes.stream().anyMatch(internal -> internal.equalsIgnoreCase(typeId));
    }
}
