package nu.fgv.register.server.dto.mapper

import nu.fgv.register.server.domain.SpexCategory
import nu.fgv.register.server.dto.SpexCategoryDto
import org.mapstruct.Mapper

@Mapper(componentModel = "spring", uses = [])
interface SpexCategoryMapper : EntityMapper<SpexCategoryDto, SpexCategory> {

    fun fromId(id: Long?): SpexCategory? {
        if (id == null) {
            return null
        }
        val spexCategory = SpexCategory()
        spexCategory.id = id
        return spexCategory
    }
}
