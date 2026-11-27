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

package nu.fgv.register.server.spexare.membership;

import nu.fgv.register.server.settings.TypeDto;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.sync.MedlemskapProperties;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * Whether a membership type is Medlemskap's (internal, a read-only copy here) or kept here (external).
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Controller
public class MembershipKindGraphqlApi {

    private final MedlemskapProperties properties;

    public MembershipKindGraphqlApi(final MedlemskapProperties properties) {
        this.properties = properties;
    }

    @SchemaMapping(typeName = "Type", field = "membershipKind")
    public String membershipKind(final TypeDto type) {
        if (type.getType() != TypeType.MEMBERSHIP) {
            return null;
        }
        return properties.isInternal(type.getId()) ? "INTERNAL" : "EXTERNAL";
    }
}
