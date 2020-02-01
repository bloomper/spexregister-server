package nu.fgv.register.server.respository.search

import nu.fgv.register.server.repository.search.SpexCategorySearchRepository
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Configuration

@Configuration
class SpexCategorySearchRepositoryMockConfiguration {

    @MockBean
    private lateinit var mockSpexCategorySearchRepository: SpexCategorySearchRepository
}
