package nu.fgv.register.server.spexare.address;

import nu.fgv.register.server.settings.TypeDto;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AddressApi.class)
public class AddressApiTest extends AbstractApiTest {

    @MockBean
    private AddressService service;

    private static final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the address"),
            fieldWithPath("streetAddress").description("The street address of the address"),
            fieldWithPath("postalCode").description("The postal code of the address"),
            fieldWithPath("city").description("The city of the address"),
            fieldWithPath("country").description("The country of the address"),
            fieldWithPath("phone").description("The phone the address"),
            fieldWithPath("phoneMobile").description("The phone (mobile) of the address"),
            fieldWithPath("emailAddress").description("The email address of the address"),
            linksSubsection
    ).andWithPrefix("type.", Stream.of(typeResponseFieldDescriptors, auditResponseFieldsDescriptors).flatMap(Collection::stream).collect(Collectors.toList()));

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("spexare").description("Link to the current spexare"),
            linkWithRel("addresses").description("Link to the current spexare's addresses")
    );

    @Test
    public void should_get_paged() throws Exception {
        var address1 = AddressDto.builder().id(1L).streetAddress("Street1").type(TypeDto.builder().id("HOME").type(TypeType.ADDRESS).build()).build();
        var address2 = AddressDto.builder().id(2L).streetAddress("Street2").type(TypeDto.builder().id("WORK").type(TypeType.ADDRESS).build()).build();

        when(service.findBySpexare(any(Long.class), any(String.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(address1, address2), PageRequest.of(1, 2, Sort.by("type")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/addresses?page=1&size=2&sort=type,desc&filter=streetAddress:whatever", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.addresses", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/addresses/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare")
                                ),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.addresses[]").description("The elements"),
                                        fieldWithPath("_embedded.addresses[].id").description("The id of the address"),
                                        fieldWithPath("_embedded.addresses[].streetAddress").description("The street address of the address"),
                                        fieldWithPath("_embedded.addresses[].postalCode").description("The postal code of the address"),
                                        fieldWithPath("_embedded.addresses[].city").description("The city of the address"),
                                        fieldWithPath("_embedded.addresses[].country").description("The country of the address"),
                                        fieldWithPath("_embedded.addresses[].phone").description("The phone of the address"),
                                        fieldWithPath("_embedded.addresses[].phoneMobile").description("The phone (mobile) of the address"),
                                        fieldWithPath("_embedded.addresses[].emailAddress").description("The email address of the address"),
                                        fieldWithPath("_embedded.addresses[].type").description("The type of the address"),
                                        fieldWithPath("_embedded.addresses[].createdBy").description("Who created the address"),
                                        fieldWithPath("_embedded.addresses[].createdAt").description("When was the address created"),
                                        fieldWithPath("_embedded.addresses[].lastModifiedBy").description("Who last modified the address"),
                                        fieldWithPath("_embedded.addresses[].lastModifiedAt").description("When was the address last modified"),
                                        subsectionWithPath("_embedded.addresses[]._links").description("The address links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters.and(filterQueryParameterDescriptors),
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get() throws Exception {
        var address = AddressDto.builder().id(1L).streetAddress("Street1").type(TypeDto.builder().id("HOME").type(TypeType.ADDRESS).build()).build();

        when(service.findById(any(Long.class), any(Long.class))).thenReturn(Optional.of(address));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{spexareId}/addresses/{id}", 1L, 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/addresses/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("id").description("The id of the address")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_create() throws Exception {
        var fields = new ConstrainedFields(AddressCreateDto.class);
        var dto = AddressCreateDto.builder().streetAddress("Street1").build();

        when(service.create(any(Long.class), any(String.class), any(AddressCreateDto.class))).thenReturn(Optional.of(AddressDto.builder().id(1L).streetAddress(dto.getStreetAddress()).type(TypeDto.builder().id("HOME").type(TypeType.ADDRESS).build()).build()));

        mockMvc
                .perform(
                        post("/api/v1/spexare/{spexareId}/addresses/{typeId}", 1L, "HOME")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/addresses/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the address")
                                ),
                                requestFields(
                                        fields.withPath("streetAddress").description("The street address of the address"),
                                        fields.withPath("postalCode").description("The postal code of the address"),
                                        fields.withPath("city").description("The city of the address"),
                                        fields.withPath("country").description("The country of the address"),
                                        fields.withPath("phone").description("The phone the address"),
                                        fields.withPath("phoneMobile").description("The phone (mobile) of the address"),
                                        fields.withPath("emailAddress").description("The email address of the address")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                createResponseHeaders
                        )
                );
    }

    @Test
    public void should_update() throws Exception {
        var fields = new ConstrainedFields(AddressUpdateDto.class);
        var address = AddressDto.builder().id(1L).streetAddress("Street1").type(TypeDto.builder().id("HOME").type(TypeType.ADDRESS).build()).build();
        var dto = AddressUpdateDto.builder().id(1L).streetAddress("Street1").city("city").build();

        when(service.update(any(Long.class), any(String.class), any(Long.class), any(AddressUpdateDto.class))).thenReturn(Optional.of(address));

        mockMvc
                .perform(
                        put("/api/v1/spexare/{spexareId}/addresses/{typeId}/{id}", 1L, "HOME", dto.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/addresses/update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the address"),
                                        parameterWithName("id").description("The id of the address")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the address"),
                                        fields.withPath("streetAddress").description("The street address of the address"),
                                        fields.withPath("postalCode").description("The postal code of the address"),
                                        fields.withPath("city").description("The city of the address"),
                                        fields.withPath("country").description("The country of the address"),
                                        fields.withPath("phone").description("The phone the address"),
                                        fields.withPath("phoneMobile").description("The phone (mobile) of the address"),
                                        fields.withPath("emailAddress").description("The email address of the address")
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_partial_update() throws Exception {
        var fields = new ConstrainedFields(AddressUpdateDto.class);
        var address = AddressDto.builder().id(1L).streetAddress("Street1").type(TypeDto.builder().id("HOME").type(TypeType.ADDRESS).build()).build();
        var dto = AddressUpdateDto.builder().id(1L).streetAddress("Street1").city("city").build();

        when(service.partialUpdate(any(Long.class), any(String.class), any(Long.class), any(AddressUpdateDto.class))).thenReturn(Optional.of(address));

        mockMvc
                .perform(
                        patch("/api/v1/spexare/{spexareId}/addresses/{typeId}/{id}", 1L, "HOME", dto.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/addresses/partial-update",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the address"),
                                        parameterWithName("id").description("The id of the address")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the address"),
                                        fields.withPath("streetAddress").description("The street address of the address").optional(),
                                        fields.withPath("postalCode").description("The postal code of the address").optional(),
                                        fields.withPath("city").description("The city of the address").optional(),
                                        fields.withPath("country").description("The country of the address").optional(),
                                        fields.withPath("phone").description("The phone the address").optional(),
                                        fields.withPath("phoneMobile").description("The phone (mobile) of the address").optional(),
                                        fields.withPath("emailAddress").description("The email address of the address").optional()
                                ),
                                responseFields,
                                links,
                                secureRequestHeaders,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete() throws Exception {
        when(service.deleteById(any(Long.class), any(String.class), any(Long.class))).thenReturn(true);

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{spexareId}/addresses/{typeId}/{id}", 1L, "HOME", 1L)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                )
                .andExpect(status().isNoContent())
                .andDo(document(
                                "spexare/addresses/delete",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("spexareId").description("The id of the spexare"),
                                        parameterWithName("typeId").description("The type id of the address"),
                                        parameterWithName("id").description("The id of the address")
                                ),
                                secureRequestHeaders
                        )
                );
    }

}
