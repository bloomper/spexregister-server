package nu.fgv.register.server.repository

import nu.fgv.register.server.model.SpexCategory
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource
interface SpexCategoryRepository : PagingAndSortingRepository<SpexCategory, Long> {
}