package nu.fgv.register.server.spexare.activity.spex;

import nu.fgv.register.server.spex.SpexApi;
import nu.fgv.register.server.spex.SpexDto;
import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SpexActivityApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class SpexActivityApiTest extends AbstractApiTest {

    @MockBean
    private SpexActivityService service;

    @MockBean
    private SpexApi spexApi;

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex activity"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("activities").description("Link to the current spexare's activities"),
            linkWithRel("spex-activities").description("Link to the current spexare's spex activities"),
            linkWithRel("spex").description("Link to the current spex")
    );

    private final ResponseFieldsSnippet spexResponseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spex"),
            fieldWithPath("year").description("The year of the spex"),
            fieldWithPath("title").description("The title of the spex"),
            fieldWithPath("revival").description("The revival flag of the spex"),
            linksSubsection
    );

    private final LinksSnippet spexLinks = baseLinks.and(
            linkWithRel("poster").description("Link to the current spex's poster").optional(),
            linkWithRel("parent").description("Link to the current spex's parent").optional(),
            linkWithRel("revivals").description("Link to the current spex's revivals").optional(),
            linkWithRel("category").description("Link to the current spex's spex category").optional(),
            linkWithRel("spex").description("Link to paged spex").optional(),
            linkWithRel("spex-including-revivals").description("Link to paged spex (including revivals)").optional()
    );

    @Test
    public void should_get_paged() throws Exception {
        var spexActivity1 = SpexActivityDto.builder().id(1L).build();
        var spexActivity2 = SpexActivityDto.builder().id(2L).build();

        when(service.findByActivity(any(Long.class), any(Long.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(spexActivity1, spexActivity2), PageRequest.of(1, 2, Sort.by("id")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activities?page=1&size=2&sort=id,desc", 1, 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spex-activities", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/activities/spex/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spex-activities[]").description("The elements"),
                                        fieldWithPath("_embedded.spex-activities[].id").description("The id of the spex activity"),
                                        fieldWithPath("_embedded.spex-activities[].createdBy").description("Who created the spex activity"),
                                        fieldWithPath("_embedded.spex-activities[].createdAt").description("When was the spex activity created"),
                                        fieldWithPath("_embedded.spex-activities[].lastModifiedBy").description("Who last modified the spex activity"),
                                        fieldWithPath("_embedded.spex-activities[].lastModifiedAt").description("When was the spex activity last modified"),
                                        subsectionWithPath("_embedded.spex-activities[]._links").description("The spex activity links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get() throws Exception {
        var spexActivity = SpexActivityDto.builder().id(1L).build();

        when(service.findById(any(Long.class), any(Long.class), any(Long.class))).thenReturn(Optional.of(spexActivity));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activities/{id}", 1, 1, 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/activities/spex/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("id").description("The id of the spex activity")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_create() throws Exception {
        var spexActivity = SpexActivityDto.builder().id(1L).build();

        when(service.create(any(Long.class), any(Long.class), any(Long.class))).thenReturn(Optional.of(spexActivity));

        mockMvc
                .perform(
                        post("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activities/{spexId}", 1, 1, 1)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/activities/spex/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("spexId").description("The id of the spex")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update() throws Exception {
        when(service.update(any(Long.class), any(Long.class), any(Long.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        put("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activities/{id}/{spexId}", 1, 1, 1, 2)
                )
                .andExpect(status().isAccepted())
                .andDo(document(
                                "spexare/activities/spex/update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("spexId").description("The id of the spex"),
                                        parameterWithName("id").description("The id of the spex activity")
                                )
                        )
                );
    }

    @Test
    public void should_delete() throws Exception {
        when(service.deleteById(any(Long.class), any(Long.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activities/{id}", 1, 1, 1)
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare/activities/spex/delete",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("id").description("The id of the spex activity")
                                )
                        )
                );
    }

    @Test
    public void should_get_spex() throws Exception {
        var spex = SpexDto.builder().id(1L).year("2021").build();
        var realSpexApi = new SpexApi(null, null, null, null);

        when(service.findSpexBySpexActivity(any(Long.class), any(Long.class), any(Long.class))).thenReturn(Optional.of(spex));
        when(spexApi.getLinks(any(SpexDto.class))).thenReturn(realSpexApi.getLinks(spex));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/activities/{activityId}/spex-activities/{id}/spex", 1, 1, 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/activities/spex/get-spex",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("activityId").description("The id of the activity"),
                                        parameterWithName("id").description("The id of the spex activity")
                                ),
                                spexResponseFields,
                                spexLinks,
                                responseHeaders
                        )
                );
    }

}
