package nu.fgv.register.server.service

import java.util.*
import nu.fgv.register.server.dto.SpexCategoryDto
import nu.fgv.register.server.dto.mapper.SpexCategoryMapper
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
class SpexCategoryService(
    private val repository: SpexCategoryRepository,
    private val mapper: SpexCategoryMapper,
    private val searchRepository: SpexCategorySearchRepository
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = getLogger(javaClass.enclosingClass)
    }

    fun save(spexCategoryDto: SpexCategoryDto): SpexCategoryDto {
        log.debug("Request to save spex category : {}") { spexCategoryDto }

        var spexCategory = mapper.toEntity(spexCategoryDto)
        val result = repository.save(spexCategory)
        searchRepository.save(result)
        return mapper.toDto(result)
    }

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<SpexCategoryDto> {
        log.debug("Request to get all spex categories")
        return repository.findAll(pageable).map(mapper::toDto)
    }

    @Transactional(readOnly = true)
    fun findOne(id: Long): Optional<SpexCategoryDto> {
        log.debug("Request to get spex category : {}") { id }
        return repository.findById(id).map(mapper::toDto)
    }

    fun delete(id: Long) {
        log.debug("Request to delete spex category : {}") { id }
        repository.deleteById(id)
        searchRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun search(query: String, pageable: Pageable): Page<SpexCategoryDto>? {
        log.debug("Request to search for a page of spex categories for query {}") { query }
        return searchRepository.search(queryStringQuery(query), pageable).map(mapper::toDto)
    }
}
