package nu.fgv.register.server.web.rest

import nu.fgv.register.server.service.SpexCategoryService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/spex-category")
class SpexCategoryResource(private val service: SpexCategoryService)
