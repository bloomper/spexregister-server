package nu.fgv.register.server.spex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SpexApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureRestDocs(outputDir = "build/snippets")
public class SpexApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpexService service;

    @Test
    public void shouldReturnListOfSpex() throws Exception {
        var spex1 = SpexDto.builder().id(1L).year("2021").build();
        var spex2 = SpexDto.builder().id(1L).year("2022").build();
        when(service.find(false, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(spex1, spex2)));
        this.mockMvc.perform(get("/api/v1/spex"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].year", is(spex1.getYear())))
                .andDo(document("spex"));
    }
}
