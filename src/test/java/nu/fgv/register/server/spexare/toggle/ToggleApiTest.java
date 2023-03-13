package nu.fgv.register.server.spexare.toggle;

import nu.fgv.register.server.settings.TypeDto;
import nu.fgv.register.server.settings.TypeType;
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

@WebMvcTest(value = ToggleApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class ToggleApiTest extends AbstractApiTest {

    @MockBean
    private ToggleService service;

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the toggle"),
            fieldWithPath("value").description("The value of the toggle"),
            linksSubsection
    ).andWithPrefix("type.", Stream.of(typeResponseFieldDescriptors, auditResponseFieldsDescriptors).flatMap(Collection::stream).collect(Collectors.toList()));

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("toggles").description("Link to the current spexare's toggles")
    );

    @Test
    public void should_get_paged() throws Exception {
        var toggle1 = ToggleDto.builder().id(1L).value(true).type(TypeDto.builder().id("DECEASED").type(TypeType.TOGGLE).build()).build();
        var toggle2 = ToggleDto.builder().id(2L).value(false).type(TypeDto.builder().id("CHALMERS_STUDENT").type(TypeType.TOGGLE).build()).build();

        when(service.findBySpexare(any(Long.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(toggle1, toggle2), PageRequest.of(1, 2, Sort.by("type")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/toggles?page=1&size=2&sort=type,desc", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.toggles", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/toggles/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.toggles[]").description("The elements"),
                                        fieldWithPath("_embedded.toggles[].id").description("The id of the toggle"),
                                        fieldWithPath("_embedded.toggles[].value").description("The value of the toggle"),
                                        fieldWithPath("_embedded.toggles[].type").description("The type of the toggle"),
                                        fieldWithPath("_embedded.toggles[].createdBy").description("Who created the toggle"),
                                        fieldWithPath("_embedded.toggles[].createdAt").description("When was the toggle created"),
                                        fieldWithPath("_embedded.toggles[].lastModifiedBy").description("Who last modified the toggle"),
                                        fieldWithPath("_embedded.toggles[].lastModifiedAt").description("When was the toggle last modified"),
                                        subsectionWithPath("_embedded.toggles[]._links").description("The toggle links"),
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
        var toggle = ToggleDto.builder().id(1L).value(true).type(TypeDto.builder().id("DECEASED").type(TypeType.TOGGLE).build()).build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(toggle));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/toggles/{id}", 1, 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/toggles/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("id").description("The id of the toggle")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_create() throws Exception {
        var toggle = ToggleDto.builder().id(1L).value(true).type(TypeDto.builder().id("DECEASED").type(TypeType.TOGGLE).build()).build();

        when(service.create(any(Long.class), any(String.class), any(Boolean.class))).thenReturn(Optional.of(toggle));

        mockMvc
                .perform(
                        post("/api/v1/spexare/{spexareId}/toggles/{typeId}/{value}", 1, toggle.getId(), Boolean.TRUE)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/toggles/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the toggle"),
                                        parameterWithName("value").description("The value of the toggle")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update() throws Exception {
        var toggle = ToggleDto.builder().id(1L).value(true).type(TypeDto.builder().id("DECEASED").type(TypeType.TOGGLE).build()).build();

        when(service.update(any(Long.class), any(String.class), any(Long.class), any(Boolean.class))).thenReturn(Optional.of(toggle));

        mockMvc
                .perform(
                        put("/api/v1/spexare/{spexareId}/toggles/{typeId}/{id}/{value}", 1, toggle.getType().getId(), toggle.getId(), Boolean.FALSE)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/toggles/update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the toggle"),
                                        parameterWithName("id").description("The id of the toggle"),
                                        parameterWithName("value").description("The value of the toggle")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete() throws Exception {
        when(service.deleteById(any(Long.class), any(String.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/toggles/{typeId}/{id}", 1, "DECEASED", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare/toggles/delete",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the toggle"),
                                        parameterWithName("id").description("The id of the toggle")
                                )
                        )
                );
    }

}
