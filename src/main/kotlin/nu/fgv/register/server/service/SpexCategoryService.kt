package nu.fgv.register.server.service

import java.util.*
import nu.fgv.register.server.model.SpexCategory
import nu.fgv.register.server.repository.SpexCategoryRepository
import nu.fgv.register.server.repository.search.SpexCategorySearchRepository
import org.elasticsearch.index.query.QueryBuilders.queryStringQuery
import org.slf4j.LoggerFactory.getLogger
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class SpexCategoryService(
    private val repository: SpexCategoryRepository,
    private val searchRepository: SpexCategorySearchRepository
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = getLogger(javaClass.enclosingClass)
    }

    fun save(spexCategory: SpexCategory): SpexCategory {
        log.debug("Request to save spex category : {}") { spexCategory }
        val result = repository.save(spexCategory)
        searchRepository.save(result)
        return result
    }

    @Transactional(readOnly = true)
    open fun findAll(pageable: Pageable): Page<SpexCategory> {
        log.debug("Request to get all spex categories")
        return repository.findAll(pageable)
    }

    @Transactional(readOnly = true)
    open fun findOne(id: Long): Optional<SpexCategory> {
        log.debug("Request to get spex category : {}") { id }
        return repository.findById(id)
    }

    fun delete(id: Long) {
        log.debug("Request to delete spex category : {}") { id }
        repository.deleteById(id)
        searchRepository.deleteById(id)
    }

    open fun search(query: String, pageable: Pageable): Page<SpexCategory>? {
        log.debug("Request to search for a page of spex categories for query {}") { query }
        return searchRepository.search(queryStringQuery(query), pageable)
    }
}
