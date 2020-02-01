package nu.fgv.register.server.dto

import nu.fgv.register.server.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpexCategoryDtoTest {

    @Test
    fun dtoEqualsVerifier() {
        equalsVerifier(SpexCategoryDto::class)
        val spexCategoryDto1 = SpexCategoryDto()
        spexCategoryDto1.id = 1L
        val spexCategoryDto2 = SpexCategoryDto()
        assertThat(spexCategoryDto1).isNotEqualTo(spexCategoryDto2)
        spexCategoryDto2.id = spexCategoryDto1.id
        assertThat(spexCategoryDto1).isEqualTo(spexCategoryDto2)
        spexCategoryDto2.id = 2L
        assertThat(spexCategoryDto1).isNotEqualTo(spexCategoryDto2)
        spexCategoryDto1.id = null
        assertThat(spexCategoryDto1).isNotEqualTo(spexCategoryDto2)
    }
}
