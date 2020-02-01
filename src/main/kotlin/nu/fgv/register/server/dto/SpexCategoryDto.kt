package nu.fgv.register.server.dto

import java.io.Serializable
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class SpexCategoryDto(

    var id: Long? = null,

    @get: NotNull
    @get: Size(max = 255)
    var name: String? = null,

    @get: NotNull
    @get: Size(max = 4)
    @get: Pattern(regexp = "^(19|20|21)\\d{2}$")
    var firstYear: String? = null,

    var logo: ByteArray? = null,
    var logoContentType: String? = null

) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpexCategoryDto) return false
        if (other.id == null || id == null) return false

        return id == other.id
    }

    override fun hashCode() = id.hashCode()
}
