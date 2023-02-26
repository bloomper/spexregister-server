package nu.fgv.register.server.settings;

import nu.fgv.register.server.util.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.headers.RequestHeadersSnippet;
import org.springframework.restdocs.hypermedia.LinksSnippet;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
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

@WebMvcTest(value = SettingsApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class SettingsApiTest extends AbstractApiTest {

    @MockBean
    private LanguageService languageService;

    @MockBean
    private CountryService countryService;

    @MockBean
    private TypeService typeService;

    private final ResponseFieldsSnippet languageResponseFields = responseFields(
            fieldWithPath("isoCode").description("The ISO code of the language"),
            fieldWithPath("label").description("The label of the language"),
            linksSubsection
    );

    private final LinksSnippet languageLinks = baseLinks.and(
            linkWithRel("languages").description("Link to all languages")
    );

    private final ResponseFieldsSnippet countryResponseFields = responseFields(
            fieldWithPath("isoCode").description("The ISO code of the country"),
            fieldWithPath("label").description("The label of the country"),
            linksSubsection
    );

    private final LinksSnippet countryLinks = baseLinks.and(
            linkWithRel("countries").description("Link to all countries")
    );

    private final ResponseFieldsSnippet typeResponseFields = auditResponseFields.and(
            fieldWithPath("id").description("The id of the type"),
            fieldWithPath("value").description("The value of the type"),
            fieldWithPath("type").description("The type of the type"),
            fieldWithPath("label").description("The label of the type"),
            linksSubsection
    );

    private final LinksSnippet typeLinks = baseLinks.and(
            linkWithRel("types").description("Link to all types")
    );

    protected final RequestHeadersSnippet requestHeaders = requestHeaders(
            headerWithName(HttpHeaders.ACCEPT_LANGUAGE).description("The accept language header").optional()
    );

    @Nested
    @DisplayName("Language")
    class LanguageApiTest {
        @Test
        public void should_get_languages() throws Exception {
            var language1 = LanguageDto.builder().isoCode("sv").label("Svenska").build();
            var language2 = LanguageDto.builder().isoCode("en").label("Engelska").build();

            when(languageService.findAll()).thenReturn((List.of(language1, language2)));

            mockMvc
                    .perform(
                            get("/api/v1/settings/language")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("_embedded.languages", hasSize(2)))
                    .andDo(print())
                    .andDo(
                            document(
                                    "settings/language-get-all",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                    responseFields(
                                            subsectionWithPath("_embedded").description("The embedded section"),
                                            subsectionWithPath("_embedded.languages[]").description("The elements"),
                                            fieldWithPath("_embedded.languages[].isoCode").description("The ISO code of the language"),
                                            fieldWithPath("_embedded.languages[].label").description("The label of the language"),
                                            subsectionWithPath("_embedded.languages[]._links").description("The language links"),
                                            linksSubsection
                                    ),
                                    requestHeaders,
                                    responseHeaders
                            )
                    );
        }

        @Test
        public void should_get_language() throws Exception {
            var language = LanguageDto.builder().isoCode("sv").label("Svenska").build();

            when(languageService.findByIsoCode(any(String.class))).thenReturn(Optional.of(language));

            mockMvc
                    .perform(
                            get("/api/v1/settings/language/{isoCode}", "sv")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("isoCode", is(notNullValue())))
                    .andDo(print())
                    .andDo(
                            document(
                                    "settings/language-get",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                    pathParameters(
                                            parameterWithName("isoCode").description("The ISO code of the language")
                                    ),
                                    languageResponseFields,
                                    languageLinks,
                                    requestHeaders,
                                    responseHeaders
                            )
                    );
        }
    }

    @Nested
    @DisplayName("Country")
    class CountryApiTest {
        @Test
        public void should_get_countries() throws Exception {
            var country1 = CountryDto.builder().isoCode("SE").label("Sverige").build();
            var country2 = CountryDto.builder().isoCode("NO").label("Norge").build();

            when(countryService.findAll()).thenReturn((List.of(country1, country2)));

            mockMvc
                    .perform(
                            get("/api/v1/settings/country")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("_embedded.countries", hasSize(2)))
                    .andDo(print())
                    .andDo(
                            document(
                                    "settings/country-get-all",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                    responseFields(
                                            subsectionWithPath("_embedded").description("The embedded section"),
                                            subsectionWithPath("_embedded.countries[]").description("The elements"),
                                            fieldWithPath("_embedded.countries[].isoCode").description("The ISO code of the country"),
                                            fieldWithPath("_embedded.countries[].label").description("The label of the country"),
                                            subsectionWithPath("_embedded.countries[]._links").description("The country links"),
                                            linksSubsection
                                    ),
                                    requestHeaders,
                                    responseHeaders
                            )
                    );
        }

        @Test
        public void should_get_country() throws Exception {
            var country = CountryDto.builder().isoCode("SE").label("Sverige").build();

            when(countryService.findByIsoCode(any(String.class))).thenReturn(Optional.of(country));

            mockMvc
                    .perform(
                            get("/api/v1/settings/country/{isoCode}", "SE")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("isoCode", is(notNullValue())))
                    .andDo(print())
                    .andDo(
                            document(
                                    "settings/country-get",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                    pathParameters(
                                            parameterWithName("isoCode").description("The ISO code of the country")
                                    ),
                                    countryResponseFields,
                                    countryLinks,
                                    requestHeaders,
                                    responseHeaders
                            )
                    );
        }
    }

    @Nested
    @DisplayName("Type")
    class TypeApiTest {
        @Test
        public void should_get_types() throws Exception {
            var type1 = TypeDto.builder().id(1L).value("HOME").type(TypeType.ADDRESS).label("Hem").build();
            var type2 = TypeDto.builder().id(2L).value("WORK").type(TypeType.ADDRESS).label("Arbete").build();

            when(typeService.findAll()).thenReturn((List.of(type1, type2)));

            mockMvc
                    .perform(
                            get("/api/v1/settings/type")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("_embedded.types", hasSize(2)))
                    .andDo(print())
                    .andDo(
                            document(
                                    "settings/type-get-all",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                    responseFields(
                                            subsectionWithPath("_embedded").description("The embedded section"),
                                            subsectionWithPath("_embedded.types[]").description("The elements"),
                                            fieldWithPath("_embedded.types[].id").description("The id of the type"),
                                            fieldWithPath("_embedded.types[].value").description("The value of the type"),
                                            fieldWithPath("_embedded.types[].type").description("The type of the type"),
                                            fieldWithPath("_embedded.types[].label").description("The label of the type"),
                                            subsectionWithPath("_embedded.types[]._links").description("The type links"),
                                            linksSubsection
                                    ),
                                    requestHeaders,
                                    responseHeaders
                            )
                    );
        }

        @Test
        public void should_get_types_of_type() throws Exception {
            var type1 = TypeDto.builder().id(1L).value("HOME").type(TypeType.ADDRESS).label("Hem").build();
            var type2 = TypeDto.builder().id(2L).value("WORK").type(TypeType.ADDRESS).label("Arbete").build();

            when(typeService.findByType(any(TypeType.class))).thenReturn((List.of(type1, type2)));

            mockMvc
                    .perform(
                            get("/api/v1/settings/type/{type}", TypeType.ADDRESS)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("_embedded.types", hasSize(2)))
                    .andDo(print())
                    .andDo(
                            document(
                                    "settings/type-get-all-of-type",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                    responseFields(
                                            subsectionWithPath("_embedded").description("The embedded section"),
                                            subsectionWithPath("_embedded.types[]").description("The elements"),
                                            fieldWithPath("_embedded.types[].id").description("The id of the type"),
                                            fieldWithPath("_embedded.types[].value").description("The value of the type"),
                                            fieldWithPath("_embedded.types[].type").description("The type of the type"),
                                            fieldWithPath("_embedded.types[].label").description("The label of the type"),
                                            subsectionWithPath("_embedded.types[]._links").description("The type links"),
                                            linksSubsection
                                    ),
                                    requestHeaders,
                                    responseHeaders
                            )
                    );
        }

        @Test
        public void should_get_type() throws Exception {
            var type = TypeDto.builder().id(1L).value("HOME").type(TypeType.ADDRESS).label("Hem").build();

            when(typeService.findById(any(Long.class))).thenReturn(Optional.of(type));

            mockMvc
                    .perform(
                            get("/api/v1/settings/type/{type}/{id}", TypeType.ADDRESS, 1L)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("id", is(notNullValue())))
                    .andDo(print())
                    .andDo(
                            document(
                                    "settings/type-get",
                                    preprocessRequest(prettyPrint()),
                                    preprocessResponse(prettyPrint(), modifyHeaders().removeMatching(HttpHeaders.CONTENT_LENGTH)),
                                    pathParameters(
                                            parameterWithName("id").description("The id of the type"),
                                            parameterWithName("type").description("The type of the type")
                                    ),
                                    typeResponseFields,
                                    typeLinks,
                                    requestHeaders,
                                    responseHeaders
                            )
                    );
        }
    }
}
