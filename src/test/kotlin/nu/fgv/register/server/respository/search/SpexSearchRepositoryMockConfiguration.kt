package nu.fgv.register.server.respository.search

import nu.fgv.register.server.repository.search.SpexSearchRepository
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Configuration

@Configuration
class SpexSearchRepositoryMockConfiguration {

    @MockBean
    private lateinit var mockSpexSearchRepository: SpexSearchRepository
}
