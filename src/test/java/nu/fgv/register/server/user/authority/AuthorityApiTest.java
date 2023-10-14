package nu.fgv.register.server.user.authority;

import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthorityApi.class)
public class AuthorityApiTest extends AbstractApiTest {

    @MockBean
    private AuthorityService service;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the authority"),
            fieldWithPath("label").description("The label of the authority"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("authorities").description("Link to authorities").optional()
    );

    @Test
    public void should_get_all() throws Exception {
        var authority1 = AuthorityDto.builder().id("ROLE_ADMIN").label("Administrator").build();
        var authority2 = AuthorityDto.builder().id("ROLE_USER").label("User").build();

        when(service.findAll(any(Sort.class))).thenReturn(List.of(authority1, authority2));

        mockMvc
                .perform(
                        get("/api/v1/users/authorities")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.authorities", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "users/authorities/get-all",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                responseFields(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.authorities[]").description("The elements"),
                                        fieldWithPath("_embedded.authorities[].id").description("The id of the authority"),
                                        fieldWithPath("_embedded.authorities[].label").description("The type of the authority"),
                                        fieldWithPath("_embedded.authorities[].createdBy").description("Who created the authority"),
                                        fieldWithPath("_embedded.authorities[].createdAt").description("When was the authority created"),
                                        fieldWithPath("_embedded.authorities[].lastModifiedBy").description("Who last modified the authority"),
                                        fieldWithPath("_embedded.authorities[].lastModifiedAt").description("When was the authority last modified"),
                                        subsectionWithPath("_embedded.authorities[]._links").description("The event links"),
                                        linksSubsection
                                ),
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get() throws Exception {
        var authority = AuthorityDto.builder().id("ROLE_ADMIN").label("Administrator").build();

        when(service.findById(any(String.class))).thenReturn(Optional.of(authority));

        mockMvc
                .perform(
                        get("/api/v1/users/authorities/{id}", 1)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "users/authorities/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the authority")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

}
