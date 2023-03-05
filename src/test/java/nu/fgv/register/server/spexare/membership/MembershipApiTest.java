package nu.fgv.register.server.spexare.membership;

import nu.fgv.register.server.settings.SettingsApiTest;
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

@WebMvcTest(value = MembershipApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class MembershipApiTest extends AbstractApiTest {

    @MockBean
    private MembershipService service;

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the membership"),
            fieldWithPath("year").description("The year of the membership"),
            linksSubsection
    ).andWithPrefix("type.", Stream.of(SettingsApiTest.typeResponseFieldDescriptors, auditResponseFieldsDescriptors).flatMap(Collection::stream).collect(Collectors.toList()));

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("memberships").description("Link to the current spexare's memberships")
    );

    @Test
    public void should_get_paged_memberships_by_spexare() throws Exception {
        var membership1 = MembershipDto.builder().id(1L).year("2022").type(TypeDto.builder().id("FGV").type(TypeType.MEMBERSHIP).build()).build();
        var membership2 = MembershipDto.builder().id(2L).year("2023").type(TypeDto.builder().id("FGV").type(TypeType.MEMBERSHIP).build()).build();

        when(service.findBySpexare(any(Long.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(membership1, membership2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/memberships?page=1&size=2&sort=year,desc", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.memberships", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/memberships/get-paged-by-spexare",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.memberships[]").description("The elements"),
                                        fieldWithPath("_embedded.memberships[].id").description("The id of the membership"),
                                        fieldWithPath("_embedded.memberships[].year").description("The year of the membership"),
                                        fieldWithPath("_embedded.memberships[].type").description("The type of the membership"),
                                        fieldWithPath("_embedded.memberships[].createdBy").description("Who created the membership"),
                                        fieldWithPath("_embedded.memberships[].createdAt").description("When was the membership created"),
                                        fieldWithPath("_embedded.memberships[].lastModifiedBy").description("Who last modified the membership"),
                                        fieldWithPath("_embedded.memberships[].lastModifiedAt").description("When was the membership last modified"),
                                        subsectionWithPath("_embedded.memberships[]._links").description("The membership links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_paged_memberships_by_spexare_and_type() throws Exception {
        var membership1 = MembershipDto.builder().id(1L).year("2022").type(TypeDto.builder().id("FGV").type(TypeType.MEMBERSHIP).build()).build();
        var membership2 = MembershipDto.builder().id(2L).year("2023").type(TypeDto.builder().id("FGV").type(TypeType.MEMBERSHIP).build()).build();

        when(service.findBySpexareAndType(any(Long.class), any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(membership1, membership2), PageRequest.of(1, 2, Sort.by("year")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/memberships/type/{type}?page=1&size=2&sort=year,desc", 1, "FGV")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.memberships", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/memberships/get-paged-by-spexare-and-type",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("type").description("The value of the type")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.memberships[]").description("The elements"),
                                        fieldWithPath("_embedded.memberships[].id").description("The id of the membership"),
                                        fieldWithPath("_embedded.memberships[].year").description("The year of the membership"),
                                        fieldWithPath("_embedded.memberships[].type").description("The type of the membership"),
                                        fieldWithPath("_embedded.memberships[].createdBy").description("Who created the membership"),
                                        fieldWithPath("_embedded.memberships[].createdAt").description("When was the membership created"),
                                        fieldWithPath("_embedded.memberships[].lastModifiedBy").description("Who last modified the membership"),
                                        fieldWithPath("_embedded.memberships[].lastModifiedAt").description("When was the membership last modified"),
                                        subsectionWithPath("_embedded.memberships[]._links").description("The membership links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_membership() throws Exception {
        var membership = MembershipDto.builder().id(1L).year("2022").type(TypeDto.builder().id("FGV").type(TypeType.MEMBERSHIP).build()).build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(membership));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/memberships/{id}", 1, 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/memberships/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("id").description("The id of the membership")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_add_membership() throws Exception {
        var membership = MembershipDto.builder().id(1L).year("2023").type(TypeDto.builder().id("FGV").type(TypeType.MEMBERSHIP).build()).build();

        when(service.addMembership(any(Long.class), any(String.class), any(String.class))).thenReturn(Optional.of(membership));

        mockMvc
                .perform(
                        put("/api/v1/spexare/{spexareId}/memberships/{type}/{year}", 1, "FGV", "2023")
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/memberships/add",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("type").description("The type of the membership"),
                                        parameterWithName("year").description("The year of the membership")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_remove_membership() throws Exception {
        when(service.removeMembership(any(Long.class), any(String.class), any(String.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/memberships/{type}/{year}", 1, "FGV", "2023")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare/memberships/remove",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("type").description("The type of the membership"),
                                        parameterWithName("year").description("The year of the membership")
                                )
                        )
                );
    }

}
