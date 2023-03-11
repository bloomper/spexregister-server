package nu.fgv.register.server.spexare;

import nu.fgv.register.server.spex.SpexUpdateDto;
import nu.fgv.register.server.util.AbstractApiTest;
import nu.fgv.register.server.util.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestBody;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SpexareApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class SpexareApiTest extends AbstractApiTest {

    @MockBean
    private SpexareService service;

    @MockBean
    private SpexareImportService importService;
    @MockBean
    private SpexareExportService exportService;

    private final ResponseFieldsSnippet responseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the spexare"),
            fieldWithPath("firstName").description("The first name of the spexare"),
            fieldWithPath("lastName").description("The last name of the spexare"),
            fieldWithPath("nickName").description("The nickname of the spexare"),
            fieldWithPath("birthDate").description("The birth date of the spexare"),
            fieldWithPath("socialSecurityNumber").description("The social security number of the spexare"),
            fieldWithPath("graduation").description("The graduation of the spexare"),
            fieldWithPath("comment").description("The comment of the spexare"),
            fieldWithPath("image").description("The image of the spexare"),
            linksSubsection
    );

    private final LinksSnippet links = baseLinks.and(
            linkWithRel("image").description("Link to the current spexare's image").optional(),
            linkWithRel("memberships").description("Link to the current spexare's memberships").optional(),
            linkWithRel("consents").description("Link to the current spexare's consents").optional(),
            linkWithRel("toggles").description("Link to the current spexare's toggles").optional(),
            linkWithRel("addresses").description("Link to the current spexare's addresses").optional()
    );

    @Test
    public void should_get_paged_spexare() throws Exception {
        var spexare1 = SpexareDto.builder().id(1L).firstName("FirstName1").lastName("LastName1").build();
        var spexare2 = SpexareDto.builder().id(2L).firstName("FirstName2").lastName("LastName2").build();

        when(service.find(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(spexare1, spexare2), PageRequest.of(1, 2, Sort.by("firstName")), 10));

        mockMvc
                .perform(
                        get("/api/v1/spexare?page=1&size=2&sort=firstName,desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("_embedded.spexare", hasSize(2)))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/get-paged",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pageLinks.and(
                                        subsectionWithPath("_embedded").description("The embedded section"),
                                        subsectionWithPath("_embedded.spexare[]").description("The elements"),
                                        fieldWithPath("_embedded.spexare[].id").description("The id of the spexare"),
                                        fieldWithPath("_embedded.spexare[].firstName").description("The first name of the spexare"),
                                        fieldWithPath("_embedded.spexare[].lastName").description("The last name of the spexare"),
                                        fieldWithPath("_embedded.spexare[].nickName").description("The nickname of the spexare"),
                                        fieldWithPath("_embedded.spexare[].image").description("The poster of the spexare"),
                                        fieldWithPath("_embedded.spexare[].createdBy").description("Who created the spexare"),
                                        fieldWithPath("_embedded.spexare[].createdAt").description("When was the spexare created"),
                                        fieldWithPath("_embedded.spexare[].lastModifiedBy").description("Who last modified the spexare"),
                                        fieldWithPath("_embedded.spexare[].lastModifiedAt").description("When was the spexare last modified"),
                                        subsectionWithPath("_embedded.spexare[]._links").description("The spexare links"),
                                        linksSubsection
                                ),
                                pagingLinks,
                                pagingQueryParameters,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_spexare_export() throws Exception {
        var export = Pair.of(".xlsx", new byte[]{10, 12});

        when(exportService.doExport(anyList(), any(String.class), any(Locale.class))).thenReturn(export);

        mockMvc
                .perform(
                        get("/api/v1/spexare?ids=1,2,3")
                                .accept(Constants.MediaTypes.APPLICATION_XLSX)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, Constants.MediaTypes.APPLICATION_XLSX_VALUE))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/get-export",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("ids").description("The ids of the spexare to export").optional()
                                ),
                                requestHeaders(
                                        headerWithName(HttpHeaders.ACCEPT).description("The content type (application/vnd.openxmlformats-officedocument.spreadsheetml.sheet and application/vnd.ms-excel supported)")
                                ),
                                responseHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header"),
                                        headerWithName(HttpHeaders.CONTENT_LENGTH).description("The content length header")
                                ),
                                responseBody()
                        )
                );
    }

    @Test
    public void should_create_spexare() throws Exception {
        var fields = new ConstrainedFields(SpexareCreateDto.class);
        var dto = SpexareCreateDto.builder().firstName("FirstName").lastName("LastName").build();

        when(service.create(any(SpexareCreateDto.class))).thenReturn(SpexareDto.builder().id(1L).firstName(dto.getFirstName()).lastName(dto.getLastName()).build());

        mockMvc
                .perform(
                        post("/api/v1/spexare")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(document(
                                "spexare/create",
                                preprocessRequest(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH).removeMatching(HttpHeaders.HOST)),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                requestFields(
                                        fields.withPath("firstName").description("The first name of the spexare"),
                                        fields.withPath("lastName").description("The last name of the spexare"),
                                        fields.withPath("nickName").description("The nickname of the spexare")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_get_spexare() throws Exception {
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{id}", 1)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/get",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_update_spexare() throws Exception {
        var fields = new ConstrainedFields(SpexareUpdateDto.class);
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();
        var dto = SpexareUpdateDto.builder().id(1L).firstName("FirstName").lastName("LastName").nickName("NickName").build();

        when(service.update(any(SpexareUpdateDto.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        put("/api/v1/spexare/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the spexare"),
                                        fields.withPath("firstName").description("The first name of the spexare"),
                                        fields.withPath("lastName").description("The last name of the spexare"),
                                        fields.withPath("nickName").description("The nickname of the spexare"),
                                        fields.withPath("birthDate").description("The birth date of the spexare"),
                                        fields.withPath("socialSecurityNumber").description("The social security number of the spexare"),
                                        fields.withPath("graduation").description("The graduation of the spexare"),
                                        fields.withPath("comment").description("The comment of the spexare")
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_partial_update_spexare() throws Exception {
        var fields = new ConstrainedFields(SpexUpdateDto.class);
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();
        var dto = SpexareUpdateDto.builder().id(1L).firstName("FirstName").lastName("LastName").nickName("NickName").build();

        when(service.partialUpdate(any(SpexareUpdateDto.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        patch("/api/v1/spexare/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("id", is(notNullValue())))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/partial-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                requestFields(
                                        fields.withPath("id").description("The id of the spexare"),
                                        fields.withPath("firstName").description("The first name of the spexare").optional(),
                                        fields.withPath("lastName").description("The last name of the spexare").optional(),
                                        fields.withPath("nickName").description("The nickname of the spexare").optional(),
                                        fields.withPath("birthDate").description("The birth date of the spexare").optional(),
                                        fields.withPath("socialSecurityNumber").description("The social security number of the spexare").optional(),
                                        fields.withPath("graduation").description("The graduation of the spexare").optional(),
                                        fields.withPath("comment").description("The comment of the spexare").optional()
                                ),
                                responseFields,
                                links,
                                responseHeaders
                        )
                );
    }

    @Test
    public void should_delete_spexare() throws Exception {
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();

        when(service.findById(any(Long.class))).thenReturn(Optional.of(spexare));
        doNothing().when(service).deleteById(any(Long.class));

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{id}", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                )
                        )
                );
    }

    @Test
    public void should_download_spexare_image() throws Exception {
        var image = Pair.of(new byte[]{10, 12}, MediaType.IMAGE_PNG_VALUE);
        when(service.getImage(any(Long.class))).thenReturn(Optional.of(image));

        mockMvc
                .perform(
                        get("/api/v1/spexare/{id}/image", 1)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, image.getSecond()))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, image.getFirst().length))
                .andDo(print())
                .andDo(
                        document(
                                "spexare/image-download",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                responseHeaders.and(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header"),
                                        headerWithName(HttpHeaders.CONTENT_LENGTH).description("The content length header")
                                ),
                                responseBody()
                        )
                );
    }

    @Test
    public void should_upload_spex_image() throws Exception {
        var image = new byte[]{10, 12};
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();
        when(service.saveImage(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        put("/api/v1/spexare/{id}/image", 1)
                                .contentType(MediaType.IMAGE_PNG)
                                .content(image)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare/image-upload",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                requestHeaders(
                                        headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type (image/png, image/jpeg and image/gif supported)")
                                ),
                                requestBody()
                        )
                );
    }

    @Test
    public void should_upload_spexare_image_via_multipart() throws Exception {
        var image = new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, new byte[]{10, 12});
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();
        when(service.saveImage(any(Long.class), any(), any(String.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        multipart("/api/v1/spexare/{id}/image", 1)
                                .file(image)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare/image-upload-multipart",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                ),
                                requestParts(
                                        partWithName("file").description("The image to upload")
                                )
                        )
                );
    }

    @Test
    public void should_delete_spexare_image() throws Exception {
        var spexare = SpexareDto.builder().id(1L).firstName("FirstName").lastName("LastName").build();
        when(service.removeImage(any(Long.class))).thenReturn(Optional.of(spexare));

        mockMvc
                .perform(
                        delete("/api/v1/spexare/{id}/image", 1)
                )
                .andExpect(status().isNoContent())
                .andDo(print())
                .andDo(
                        document(
                                "spexare/image-delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                pathParameters(
                                        parameterWithName("id").description("The id of the spexare")
                                )
                        )
                );
    }

}
