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

@WebMvcTest(value = SpexCategoryApi.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureRestDocs(outputDir = "build/snippets")
public class SpexCategoryApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpexCategoryService service;

    @Test
    public void shouldReturnListOfSpexCategories() throws Exception {
        var category1 = SpexCategory.builder().id(1L).name("category1").build();
        var category2 = SpexCategory.builder().id(1L).name("category2").build();
        when(service.find(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(category1, category2)));
        this.mockMvc.perform(get("/api/v1/spex/category"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is(category1.getName())))
                .andDo(document("spexCategory"));
    }
}
