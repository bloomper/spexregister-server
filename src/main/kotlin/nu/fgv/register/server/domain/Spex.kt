package nu.fgv.register.server.domain

import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.elasticsearch.annotations.Document

@Entity
@Table(name = "spex")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "spex")
data class Spex(
    @get: NotNull
    @get: Size(max = 4)
    @get: Pattern(regexp = "^(19|20|21)\\d{2}$")
    var year: String? = null,
    @ManyToOne(optional = false)
    var category: SpexCategory? = null,
    @ManyToOne
    var parent: Spex? = null,
    @ManyToOne(optional = false)
    var details: SpexDetails? = null
) : Base() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Spex) return false
        if (other.id == null || id == null) return false

        return id == other.id
    }

    override fun hashCode() = 31
}
