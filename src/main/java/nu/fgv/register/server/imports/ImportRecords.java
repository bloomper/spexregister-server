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

package nu.fgv.register.server.imports;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The records {@code importBatch} accepts (spexregister-spec, docs/migration.md, 3.1), as GraphQL binds them. Other
 * records of the batch are referred to by source key; spex, tasks and characters by their Spexipedia id.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public final class ImportRecords {

    private ImportRecords() {
    }

    /** One call, applied in this order: tags, spexare, partners, activities, news, users. */
    public record Batch(@Nullable List<Tag> tags, @Nullable List<Spexare> spexare, @Nullable List<Partner> partners,
                        @Nullable List<Activity> activities, @Nullable List<News> news, @Nullable List<User> users) {

        int size() {
            return count(tags) + count(spexare) + count(partners) + count(activities) + count(news) + count(users);
        }

        private static int count(final @Nullable List<?> list) {
            return list == null ? 0 : list.size();
        }
    }

    public record Tag(String sourceKey, String name) {
    }

    public record Spexare(String sourceKey, String firstName, String lastName, @Nullable String nickName,
                          @Nullable String socialSecurityNumber, boolean deceased, boolean published,
                          @Nullable String graduation, @Nullable String comment, @Nullable File image,
                          @Nullable List<Address> addresses, @Nullable List<TypeValue> consents,
                          @Nullable List<TypeValue> toggles, @Nullable List<String> tags,
                          @Nullable List<Membership> memberships) {
    }

    public record File(String sha256, @Nullable String name) {
    }

    public record Address(String type, @Nullable String streetAddress, @Nullable String postalCode, @Nullable String city,
                          @Nullable String country, @Nullable String phone, @Nullable String phoneMobile,
                          @Nullable String emailAddress) {
    }

    public record TypeValue(String type, boolean value) {
    }

    public record Membership(String type, String year) {
    }

    public record Partner(String sourceKey, String spexare, String partner) {
    }

    public record Activity(String sourceKey, String spexare, UUID spexId, @Nullable UUID taskId, @Nullable List<Actor> actors) {
    }

    public record Actor(String vocal, @Nullable String role, @Nullable UUID characterId) {
    }

    public record News(String sourceKey, String subject, String text, @Nullable LocalDate visibleFrom, @Nullable LocalDate visibleTo) {
    }

    public record User(String sourceKey, String externalId, String state, @Nullable String spexare) {
    }
}
