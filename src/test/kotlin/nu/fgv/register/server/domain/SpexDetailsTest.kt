package nu.fgv.register.server.domain

import nu.fgv.register.server.equalsVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SpexDetailsTest {

    @Test
    fun equalsVerifier() {
        equalsVerifier(Spex::class)
        val spex1 = Spex()
        spex1.id = 1L
        val spex2 = Spex()
        spex2.id = spex1.id
        assertThat(spex1).isEqualTo(spex2)
        spex2.id = 2L
        assertThat(spex1).isNotEqualTo(spex2)
        spex1.id = null
        assertThat(spex1).isNotEqualTo(spex2)
    }
}
