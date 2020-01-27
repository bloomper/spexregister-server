package nu.fgv.register.server.service

import java.util.*
import nu.fgv.register.server.model.Spex
import nu.fgv.register.server.repository.SpexRepository
import nu.fgv.register.server.repository.search.SpexSearchRepository
import org.elasticsearch.index.query.QueryBuilders.queryStringQuery
import org.slf4j.LoggerFactory.getLogger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class SpexService(
    private val repository: SpexRepository,
    private val searchRepository: SpexSearchRepository
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = getLogger(javaClass.enclosingClass)
    }

    fun save(spex: Spex): Spex {
        log.debug("Request to save spex : {}") { spex }
        val result = repository.save(spex)
        searchRepository.save(result)
        return result
    }

    @Transactional(readOnly = true)
    open fun findAll(pageable: Pageable): Page<Spex> {
        log.debug("Request to get all spex")
        return repository.findAll(pageable)
    }

    @Transactional(readOnly = true)
    open fun findOne(id: Long): Optional<Spex> {
        log.debug("Request to get spex : {}") { id }
        return repository.findById(id)
    }

    fun delete(id: Long) {
        log.debug("Request to delete spex : {}") { id }
        repository.deleteById(id)
        searchRepository.deleteById(id)
    }

    open fun search(query: String, pageable: Pageable): Page<Spex>? {
        log.debug("Request to search for a page of spex for query {}") { query }
        return searchRepository.search(queryStringQuery(query), pageable)
    }
}
