package nu.fgv.register.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import nu.fgv.register.server.config.SpexregisterConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.headers.RequestHeadersSnippet;
import org.springframework.restdocs.headers.ResponseHeadersSnippet;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.payload.SubsectionDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.QueryParametersSnippet;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.util.StringUtils.collectionToDelimitedString;

@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@ExtendWith(RestDocumentationExtension.class)
@EnableConfigurationProperties(value = SpexregisterConfig.class)
public abstract class AbstractApiTest {

    protected static final LinksSnippet baseLinks = links(
            halLinks(),
            linkWithRel("self").description("Link to the current entity")
    );

    protected static final LinksSnippet pagingLinks = links(
            halLinks(),
            linkWithRel("first").description("Link to the first page"),
            linkWithRel("next").description("Link to the next page"),
            linkWithRel("prev").description("Link to the previous page"),
            linkWithRel("last").description("Link to the last page"),
            linkWithRel("self").description("Link to the current page")
    );

    protected static final ResponseFieldsSnippet pageLinks = responseFields(
            subsectionWithPath("page").description("Page section"),
            fieldWithPath("page.size").description("The size of one page"),
            fieldWithPath("page.totalElements").description("The total number of elements found"),
            fieldWithPath("page.totalPages").description("The total number of pages"),
            fieldWithPath("page.number").description("The current page number")
    );

    protected static final QueryParametersSnippet pagingQueryParameters = queryParameters(
            parameterWithName("page").description("The page to be requested"),
            parameterWithName("size").description("Parameter determining the size of the requested page"),
            parameterWithName("sort").description("Information about sorting elements")
    );

    protected static final QueryParametersSnippet sortQueryParameters = queryParameters(
            parameterWithName("sort").description("Information about sorting elements")
    );

    protected static final List<ParameterDescriptor> filterQueryParameterDescriptors = List.of(
            parameterWithName("filter").description("Parameter determining the filtering").optional()
    );

    protected static final RequestHeadersSnippet secureRequestHeaders = requestHeaders(
            headerWithName(HttpHeaders.AUTHORIZATION).description("The authorization header")
    );

    protected static final ResponseHeadersSnippet responseHeaders = responseHeaders(
            headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header")
    );

    protected static final ResponseHeadersSnippet createResponseHeaders = responseHeaders.and(
            headerWithName(HttpHeaders.LOCATION).description("The location header")
    );

    protected static final ResponseHeadersSnippet createOnlyResponseHeaders = responseHeaders(
            headerWithName(HttpHeaders.LOCATION).description("The location header")
    );

    protected static final List<FieldDescriptor> auditResponseFieldsDescriptors = List.of(
            fieldWithPath("createdBy").description("Who created the entity"),
            fieldWithPath("createdAt").description("When was the entity created"),
            fieldWithPath("lastModifiedBy").description("Who last modified the entity"),
            fieldWithPath("lastModifiedAt").description("When was the entity last modified")
    );
    protected static final ResponseFieldsSnippet auditResponseFields = responseFields(
            auditResponseFieldsDescriptors
    );

    protected static final List<FieldDescriptor> typeResponseFieldDescriptors = List.of(
            fieldWithPath("id").description("The id of the type"),
            fieldWithPath("type").description("The type of the type"),
            fieldWithPath("label").description("The label of the type")
    );

    protected static final SubsectionDescriptor linksSubsection = (SubsectionDescriptor) subsectionWithPath("_links").description("Links section");

    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation)
                        .uris()
                        .withHost("register.fgv.nu")
                        .withPort(443)
                        .withScheme("https"))
                .build();
    }

    protected static class ConstrainedFields {

        private final ConstraintDescriptions constraintDescriptions;

        public ConstrainedFields(Class<?> input) {
            this.constraintDescriptions = new ConstraintDescriptions(input);
        }

        public FieldDescriptor withPath(final String path) {
            return fieldWithPath(path)
                    .attributes(key("constraints")
                            .value(collectionToDelimitedString(this.constraintDescriptions.descriptionsForProperty(path), ". ")));
        }
    }
}
