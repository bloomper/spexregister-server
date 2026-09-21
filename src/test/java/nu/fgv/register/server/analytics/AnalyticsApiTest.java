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

package nu.fgv.register.server.analytics;

import nu.fgv.register.server.analytics.AnalyticsDto.DataQualityDto;
import nu.fgv.register.server.analytics.AnalyticsDto.DemographicsDto;
import nu.fgv.register.server.analytics.AnalyticsDto.LifecycleDto;
import nu.fgv.register.server.analytics.AnalyticsDto.OperationsDto;
import nu.fgv.register.server.analytics.AnalyticsDto.ParticipationDto;
import nu.fgv.register.server.analytics.AnalyticsDto.TotalsDto;
import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@WebMvcTest(value = AnalyticsApi.class)
class AnalyticsApiTest extends AbstractApiTest {

    private static final String BUCKET_DESCRIPTION = "A breakdown slice: `key` and `facet` name the spexare search filter that opens the records behind it (`f.<facet>=<key>`), and `facet` is null when the slice has no list";

    private final ResponseFieldsSnippet responseFields = responseFields(
            subsectionWithPath("totals").description("The register's headline counts, each with three years of monthly growth"),
            fieldWithPath("totals.spexareCount").description("The total number of published spexare"),
            fieldWithPath("totals.spexareCountHistory").description("History of the number of spexare per month"),
            fieldWithPath("totals.spexareCountHistory[].label").description("The month and year (YYYY-MM)"),
            fieldWithPath("totals.spexareCountHistory[].count").description("The count for that month"),
            fieldWithPath("totals.userCount").description("The total number of users"),
            subsectionWithPath("totals.userCountHistory").description("History of the number of users per month"),
            fieldWithPath("totals.spexCount").description("The total number of spex"),
            subsectionWithPath("totals.spexCountHistory").description("History of the number of spex per month"),
            fieldWithPath("totals.spexRevivalCount").description("The total number of spex revivals"),
            subsectionWithPath("totals.spexRevivalCountHistory").description("History of the number of spex revivals per month"),
            fieldWithPath("totals.taskCount").description("The total number of tasks"),
            subsectionWithPath("totals.taskCountHistory").description("History of the number of tasks per month"),
            subsectionWithPath("participation").description("Who took part in what"),
            fieldWithPath("participation.bySpexYear").description("Spexare per spex year. " + BUCKET_DESCRIPTION),
            fieldWithPath("participation.bySpexYear[].key").description("The bucket's filter value"),
            fieldWithPath("participation.bySpexYear[].label").description("The bucket's display label"),
            fieldWithPath("participation.bySpexYear[].count").description("The number of spexare in the bucket"),
            fieldWithPath("participation.bySpexYear[].facet").description("The search facet the bucket filters on, or null"),
            subsectionWithPath("participation.bySpexCategory").description("Spexare per spex category"),
            subsectionWithPath("participation.topSpex").description("The spex with the most participants"),
            subsectionWithPath("participation.byTaskCategory").description("Spexare per task category"),
            subsectionWithPath("participation.topTask").description("The most commonly held tasks"),
            subsectionWithPath("participation.byVocal").description("Spexare per vocal"),
            subsectionWithPath("demographics").description("Who they are and how they can be reached"),
            subsectionWithPath("demographics.byCountry").description("Spexare per country of address"),
            subsectionWithPath("demographics.byAddressType").description("Spexare per address type"),
            subsectionWithPath("demographics.byMembership").description("Spexare per membership"),
            subsectionWithPath("demographics.byTag").description("Spexare per tag"),
            subsectionWithPath("demographics.byToggle").description("Spexare per toggle"),
            subsectionWithPath("demographics.byStatus").description("Living and deceased spexare"),
            fieldWithPath("demographics.consentCompletion").description("How far each consent type has been answered"),
            fieldWithPath("demographics.consentCompletion[].key").description("The consent type id"),
            fieldWithPath("demographics.consentCompletion[].label").description("The consent type label"),
            fieldWithPath("demographics.consentCompletion[].granted").description("The number of spexare who granted it"),
            fieldWithPath("demographics.consentCompletion[].denied").description("The number of spexare who denied it"),
            fieldWithPath("demographics.consentCompletion[].missing").description("The number of spexare who have answered neither way"),
            subsectionWithPath("lifecycle").description("Arrival, tenure and departure"),
            subsectionWithPath("lifecycle.newcomersByYear").description("Spexare per year of their first spex"),
            subsectionWithPath("lifecycle.lastActiveByYear").description("Spexare per year of their most recent spex"),
            subsectionWithPath("lifecycle.byEngagement").description("Spexare per number of spex taken part in"),
            subsectionWithPath("lifecycle.byDormancy").description("Spexare per activity band; the keys are year ranges such as `2016..2022`"),
            fieldWithPath("lifecycle.oneTimers").description("The number of spexare who took part in exactly one spex"),
            fieldWithPath("lifecycle.returning").description("The number of spexare who took part in two to four spex"),
            fieldWithPath("lifecycle.veterans").description("The number of spexare who took part in five spex or more"),
            fieldWithPath("lifecycle.neverActive").description("The number of spexare with no recorded spex activity"),
            subsectionWithPath("dataQuality").description("The gaps worth chasing; null unless the caller is an editor or an administrator").optional(),
            fieldWithPath("dataQuality.total").description("The number of spexare the caller may read \u2014 the denominator the gap counts are a share of").optional().type("Number"),
            subsectionWithPath("dataQuality.issues").description("One bucket per gap, drilling into the records that have it").optional(),
            fieldWithPath("dataQuality.complete").description("An upper bound on the number of records free of every gap").optional().type("Number"),
            subsectionWithPath("operations").description("How the register is being run; null unless the caller is an administrator").optional()
    );

    @MockitoBean
    private AnalyticsService service;

    @Test
    void should_get() throws Exception {
        when(service.getAnalytics(AnalyticsSection.ALL)).thenReturn(analytics(
                DataQualityDto.builder().total(10L).issues(List.of(BucketDto.of("noAddress", "No address", 4L, "quality"))).complete(6L).build(),
                OperationsDto.builder()
                        .usersByState(List.of(BucketDto.of("ACTIVE", "Active", 5L)))
                        .usersWithoutSpexare(1L)
                        .spexareWithoutUser(4L)
                        .revisionsByMonth(List.of(BucketDto.of("2026-01", "2026-01", 12L)))
                        .revisionsBySource(List.of(BucketDto.of("WEB", "Web", 12L)))
                        .topEditors(List.of(BucketDto.of("ada", "ada", 12L)))
                        .build()));

        mockMvc
                .perform(
                        get("/api/analytics")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.spexareCount", is(10)))
                .andExpect(jsonPath("$.participation.bySpexYear[0].facet", is("spexYears")))
                .andExpect(jsonPath("$.lifecycle.byDormancy[0].key", is("2024..")))
                .andDo(print())
                .andDo(
                        document(
                                "analytics-get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields,
                                secureRequestHeaders,
                                responseHeaders,
                                security(getRolesFromMethod(AnalyticsApi.class, "getAnalytics"))
                        )
                );
    }

    @Test
    void should_omit_sections_the_caller_may_not_read() throws Exception {
        when(service.getAnalytics(AnalyticsSection.ALL)).thenReturn(analytics(null, null));

        mockMvc
                .perform(
                        get("/api/analytics")
                                .apiVersion("1.0")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participation.bySpexYear[0].count", is(7)))
                .andExpect(jsonPath("$.dataQuality", is(nullValue())))
                .andExpect(jsonPath("$.operations", is(nullValue())));
    }

    private AnalyticsDto analytics(final DataQualityDto dataQuality, final OperationsDto operations) {
        return AnalyticsDto.builder()
                .totals(TotalsDto.builder()
                        .spexareCount(10L)
                        .spexareCountHistory(List.of(new HistoryDto("2026-01", 2L)))
                        .userCount(5L)
                        .userCountHistory(List.of(new HistoryDto("2026-01", 1L)))
                        .spexCount(3L)
                        .spexCountHistory(List.of(new HistoryDto("2026-01", 1L)))
                        .spexRevivalCount(1L)
                        .spexRevivalCountHistory(List.of(new HistoryDto("2026-01", 0L)))
                        .taskCount(20L)
                        .taskCountHistory(List.of(new HistoryDto("2026-01", 5L)))
                        .build())
                .participation(ParticipationDto.builder()
                        .bySpexYear(List.of(BucketDto.of("2024", "2024", 7L, "spexYears")))
                        .bySpexCategory(List.of(BucketDto.of("chalmersspexet", "Chalmersspexet", 7L, "spexCategoryNames")))
                        .topSpex(List.of(BucketDto.of("bojan", "Bojan", 5L, "spexTitles")))
                        .byTaskCategory(List.of(BucketDto.of("scen", "Scen", 6L, "taskCategoryNames")))
                        .topTask(List.of(BucketDto.of("regi", "Regi", 3L, "taskNames")))
                        .byVocal(List.of(BucketDto.of("sopran", "Sopran", 2L, "actorVocals")))
                        .build())
                .demographics(DemographicsDto.builder()
                        .byCountry(List.of(BucketDto.of("se", "Sweden", 9L, "countries")))
                        .byAddressType(List.of(BucketDto.of("home", "Home", 9L, "addressTypes")))
                        .byMembership(List.of(BucketDto.of("ordinary:2024", "Ordinary: 2024", 8L, "memberships")))
                        .byTag(List.of(BucketDto.of("founder", "Founder", 1L, "tags")))
                        .byToggle(List.of(BucketDto.of("newsletter:true", "Newsletter: Yes", 6L, "toggles")))
                        .byStatus(List.of(BucketDto.of("false", "Living", 9L, "deceased")))
                        .consentCompletion(List.of(ConsentCompletionDto.builder()
                                .key("gdpr")
                                .label("GDPR")
                                .granted(6L)
                                .denied(1L)
                                .missing(3L)
                                .build()))
                        .build())
                .lifecycle(LifecycleDto.builder()
                        .newcomersByYear(List.of(BucketDto.of("2024", "2024", 3L, "debutYears")))
                        .lastActiveByYear(List.of(BucketDto.of("2024", "2024", 7L, "lastActiveYears")))
                        .byEngagement(List.of(BucketDto.of("1", "1", 4L, "spexCounts")))
                        .byDormancy(List.of(BucketDto.of("2024..", "Active", 7L, "lastActiveYears")))
                        .oneTimers(4L)
                        .returning(3L)
                        .veterans(1L)
                        .neverActive(2L)
                        .build())
                .dataQuality(dataQuality)
                .operations(operations)
                .build();
    }

}
