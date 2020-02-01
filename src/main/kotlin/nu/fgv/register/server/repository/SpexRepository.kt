package nu.fgv.register.server.repository

import nu.fgv.register.server.domain.Spex
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource
interface SpexRepository : PagingAndSortingRepository<Spex, Long>
