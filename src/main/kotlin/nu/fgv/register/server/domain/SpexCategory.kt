package nu.fgv.register.server.domain

import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.elasticsearch.annotations.Document

@Entity
@Table(name = "spex_categories")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "spexcategory")
data class SpexCategory(
    @get: NotNull
    @get: Size(max = 255)
    @Column(name = "name", length = 255, nullable = false)
    var name: String? = null,
    @get: NotNull
    @get: Size(max = 4)
    @get: Pattern(regexp = "^(19|20|21)\\d{2}$")
    @Column(name = "first_year", length = 4, nullable = false)
    var firstYear: String? = null,
    @Lob
    @Column(name = "logo")
    var logo: ByteArray? = null,
    @Column(name = "logo_content_type")
    var logoContentType: String? = null
) : Base() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpexCategory) return false
        if (other.id == null || id == null) return false

        return id == other.id
    }

    override fun hashCode() = 31
}
