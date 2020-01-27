package nu.fgv.register.server.repository.search

import nu.fgv.register.server.model.Spex
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface SpexSearchRepository : ElasticsearchRepository<Spex, Long>
