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

package nu.fgv.register.server.security;

import nu.fgv.register.server.news.News;
import nu.fgv.register.server.news.NewsRepository;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.util.AbstractGraphqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What each role sees through GraphQL, per kind of record, against the policies (data-model.md, Policies). The query
 * side of every policy is exercised by a list and, where the rules depend on the record, a single lookup.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class AccessPolicyMatrixIntegrationTest extends AbstractGraphqlIntegrationTest {

    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final SpexCategoryRepository spexCategoryRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final TagRepository tagRepository;
    private final SpexareRepository spexareRepository;
    private final NewsRepository newsRepository;
    private Long unpublishedSpexareId;
    private Long unpublishedNewsId;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public AccessPolicyMatrixIntegrationTest(final JdbcClient jdbcClient,
                                             final Keycloak keycloakAdminClient,
                                             final String keycloakClientId,
                                             final ObjectMapper objectMapper,
                                             final SpexRepository spexRepository,
                                             final SpexDetailsRepository spexDetailsRepository,
                                             final SpexCategoryRepository spexCategoryRepository,
                                             final TaskRepository taskRepository,
                                             final TaskCategoryRepository taskCategoryRepository,
                                             final TagRepository tagRepository,
                                             final SpexareRepository spexareRepository,
                                             final NewsRepository newsRepository) {
        super(jdbcClient, keycloakAdminClient, keycloakClientId, objectMapper);
        this.spexRepository = spexRepository;
        this.spexDetailsRepository = spexDetailsRepository;
        this.spexCategoryRepository = spexCategoryRepository;
        this.taskRepository = taskRepository;
        this.taskCategoryRepository = taskCategoryRepository;
        this.tagRepository = tagRepository;
        this.spexareRepository = spexareRepository;
        this.newsRepository = newsRepository;
    }

    @BeforeEach
    void setUp() {
        httpGraphQlTester = HttpGraphQlTester.create(
                WebTestClient.bindToServer()
                        .baseUrl("http://localhost:%s%s".formatted(localPort, graphqlPath))
                        .build()
        );

        jdbcClient.sql("UPDATE spex SET parent_id = NULL").update();
        JdbcTestUtils.deleteFromTables(jdbcClient, "spex", "spex_details", "spex_category", "task", "task_category", "tag",
                "spexare", "news", "spex_audit", "spex_details_audit", "spex_category_audit", "task_audit", "task_category_audit",
                "tag_audit", "spexare_audit", "news_audit");

        final SpexCategory spexCategory = new SpexCategory();
        spexCategory.setName("Chalmersspexet");
        spexCategory.setFirstYear("1948");
        final SpexDetails details = new SpexDetails();
        details.setTitle("Ali Baba");
        details.setCategory(spexCategoryRepository.save(spexCategory));
        final Spex spex = new Spex();
        spex.setYear("1956");
        spex.setDetails(spexDetailsRepository.save(details));
        spexRepository.save(spex);

        final TaskCategory taskCategory = new TaskCategory();
        taskCategory.setName("Ensemble");
        taskCategory.setActorPresent(true);
        final Task task = new Task();
        task.setName("Sångare");
        task.setCategory(taskCategoryRepository.save(taskCategory));
        taskRepository.save(task);

        final Tag tag = new Tag();
        tag.setName("Kör");
        tagRepository.save(tag);

        spexareRepository.save(spexare("Petra", true));
        unpublishedSpexareId = spexareRepository.save(spexare("Olle", false)).getId();

        newsRepository.save(news("Publicerad", true));
        unpublishedNewsId = newsRepository.save(news("Opublicerad", false)).getId();
    }

    @ParameterizedTest(name = "{0} sees {2} of {1}")
    @CsvSource({
            // role,  list,              count
            "USER,    spexPaged,         1",
            "EDITOR,  spexPaged,         1",
            "ADMIN,   spexPaged,         1",
            "USER,    spexCategoryPaged, 1",
            "EDITOR,  spexCategoryPaged, 1",
            "ADMIN,   spexCategoryPaged, 1",
            "USER,    taskPaged,         1",
            "EDITOR,  taskPaged,         1",
            "ADMIN,   taskPaged,         1",
            "USER,    taskCategoryPaged, 1",
            "EDITOR,  taskCategoryPaged, 1",
            "ADMIN,   taskCategoryPaged, 1",
            "USER,    tagPaged,          1",
            "EDITOR,  tagPaged,          1",
            "ADMIN,   tagPaged,          1",
            "USER,    spexarePaged,      1",
            "EDITOR,  spexarePaged,      2",
            "ADMIN,   spexarePaged,      2",
            "USER,    newsPaged,         1",
            "EDITOR,  newsPaged,         2",
            "ADMIN,   newsPaged,         2"
    })
    void should_list_what_the_policy_permits(final String role, final String list, final int count) {
        // spexarePaged and newsPaged filter on published by default; ask for everything and let the policy decide
        final String filter = "spexarePaged".equals(list) || "newsPaged".equals(list) ? "(filter: \"\", first: 100)" : "(first: 100)";

        as(role)
                .document("query { %s%s { totalCount } }".formatted(list, filter))
                .execute()
                .errors()
                .verify()
                .path(list + ".totalCount").entity(Integer.class).isEqualTo(count);
    }

    @ParameterizedTest(name = "{0} reads an unpublished {1}: {2}")
    @CsvSource({
            "USER,    spexare, false",
            "EDITOR,  spexare, true",
            "ADMIN,   spexare, true",
            "USER,    news,    false",
            "EDITOR,  news,    true",
            "ADMIN,   news,    true"
    })
    void should_look_up_what_the_policy_permits(final String role, final String type, final boolean readable) {
        final Long id = "spexare".equals(type) ? unpublishedSpexareId : unpublishedNewsId;
        final var response = as(role)
                .document("query get($id: ID!) { %s(id: $id) { id } }".formatted(type))
                .variable("id", id)
                .execute();

        if (readable) {
            response.errors().verify().path(type + ".id").entity(Long.class).isEqualTo(id);
        } else {
            response.errors()
                    .satisfy(errors -> assertThat(errors).isNotEmpty())
                    .path(type).valueIsNull();
        }
    }

    private HttpGraphQlTester as(final String role) {
        final String token = switch (role) {
            case "ADMIN" -> obtainAdminAccessToken();
            case "EDITOR" -> obtainEditorAccessToken();
            default -> obtainUserAccessToken();
        };

        return httpGraphQlTester
                .mutate()
                .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, token))
                .build();
    }

    private static Spexare spexare(final String firstName, final boolean published) {
        final Spexare spexare = new Spexare();

        spexare.setFirstName(firstName);
        spexare.setLastName("Matris");
        spexare.setDeceased(false);
        spexare.setPublished(published);

        return spexare;
    }

    private static News news(final String subject, final boolean published) {
        final News news = new News();

        news.setSubject(subject);
        news.setText("Text");
        news.setPublished(published);

        return news;
    }
}
