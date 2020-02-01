package nu.fgv.register.server.domain

import nu.fgv.register.server.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpexTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(SpexCategory::class)
        val spexCategory1 = SpexCategory()
        spexCategory1.id = 1L
        val spexCategory2 = SpexCategory()
        spexCategory2.id = spexCategory1.id
        assertThat(spexCategory1).isEqualTo(spexCategory2)
        spexCategory2.id = 2L
        assertThat(spexCategory1).isNotEqualTo(spexCategory2)
        spexCategory1.id = null
        assertThat(spexCategory1).isNotEqualTo(spexCategory2)
    }
}
