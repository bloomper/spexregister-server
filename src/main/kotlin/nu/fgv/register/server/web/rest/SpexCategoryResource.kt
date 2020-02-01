package nu.fgv.register.server.web.rest

import java.net.URI
import javax.validation.Valid
import nu.fgv.register.server.dto.SpexCategoryDto
import nu.fgv.register.server.service.SpexCategoryService
import nu.fgv.register.server.web.rest.error.BadRequestAlertException
import nu.fgv.register.server.web.util.HeaderUtil.Companion.createEntityCreationAlert
import nu.fgv.register.server.web.util.HeaderUtil.Companion.createEntityUpdateAlert
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val ENTITY_NAME = "spexCategory"

@RestController
@RequestMapping("/api/spex-category")
class SpexCategoryResource(private val service: SpexCategoryService) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    @PostMapping
    fun createSpexCategory(@Valid @RequestBody category: SpexCategoryDto): ResponseEntity<SpexCategoryDto> {
        log.debug("REST request to save spex category : {}", category)
        if (category.id != null) {
            throw BadRequestAlertException(
                "A new spex category cannot already have an ID", ENTITY_NAME, "idexists"
            )
        }
        val result = service.save(category)
        return ResponseEntity
            .created(URI("/api/spex-category/" + result.id))
            .headers(createEntityCreationAlert(ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    @PutMapping
    fun updateSpexCategory(@Valid @RequestBody category: SpexCategoryDto): ResponseEntity<SpexCategoryDto> {
        log.debug("REST request to update spex category : {}", category)
        if (category.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = service.save(category)
        return ResponseEntity
            .ok()
            .headers(createEntityUpdateAlert(ENTITY_NAME, category.id.toString()))
            .body(result)
    }

    @GetMapping
    fun getAllSpexCategories(pageable: Pageable): ResponseEntity<MutableList<SpexCategoryDto>> {
        log.debug("REST request to get a page of spex categories")
        val page = service.findAll(pageable)
        return ResponseEntity.ok().build()
        /*
        val headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page)
        return ResponseEntity
            .ok()
            .headers(headers)
            .body(page.content)
         */
    }
}
