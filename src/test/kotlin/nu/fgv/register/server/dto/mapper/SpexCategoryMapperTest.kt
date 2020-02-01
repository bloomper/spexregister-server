package nu.fgv.register.server.dto.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SpexCategoryMapperTest {

    private lateinit var mapper: SpexCategoryMapper

    @BeforeEach
    fun setUp() {
        mapper = SpexCategoryMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 2L
        assertThat(mapper.fromId(id)?.id).isEqualTo(id)
        assertThat(mapper.fromId(null)).isNull()
    }
}
