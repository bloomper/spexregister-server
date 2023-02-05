package nu.fgv.register.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.headers.ResponseHeadersSnippet;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.payload.SubsectionDescriptor;
import org.springframework.restdocs.request.QueryParametersSnippet;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
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
public abstract class AbstractApiTest {

    protected final LinksSnippet baseLinks = links(
            halLinks(),
            linkWithRel("self").description("Link to the current entity"),
            linkWithRel("logo").description("Link to the current entity's logo").optional()
    );

    protected final LinksSnippet pagingLinks = links(
            halLinks(),
            linkWithRel("first").description("Link to the first page"),
            linkWithRel("next").description("Link to the next page"),
            linkWithRel("prev").description("Link to the previous page"),
            linkWithRel("last").description("Link to the last page"),
            linkWithRel("self").description("Link to the current page")
    );

    protected final ResponseFieldsSnippet pageLinks = responseFields(
            subsectionWithPath("page").description("Page section"),
            fieldWithPath("page.size").description("The size of one page"),
            fieldWithPath("page.totalElements").description("The total number of elements found"),
            fieldWithPath("page.totalPages").description("The total number of pages"),
            fieldWithPath("page.number").description("The current page number")
    );

    protected final QueryParametersSnippet pagingQueryParameters = queryParameters(
            parameterWithName("page").description("The page to be requested"),
            parameterWithName("size").description("Parameter determining the size of the requested page"),
            parameterWithName("sort").description("Information about sorting elements")
    );

    protected final ResponseHeadersSnippet responseHeaders = responseHeaders(
            headerWithName(HttpHeaders.CONTENT_TYPE).description("The content type header")
    );

    protected final ResponseFieldsSnippet auditResponseFields = responseFields(
            fieldWithPath("createdBy").description("Who created the entity"),
            fieldWithPath("createdAt").description("When was the entity created"),
            fieldWithPath("lastModifiedBy").description("Who last modified the entity"),
            fieldWithPath("lastModifiedAt").description("When was the entity last modified")
    );

    protected final SubsectionDescriptor linksSubsection = (SubsectionDescriptor) subsectionWithPath("_links").description("Links section");

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
