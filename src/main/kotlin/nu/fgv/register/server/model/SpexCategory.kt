package nu.fgv.register.server.model

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.solr.core.mapping.SolrDocument
import javax.persistence.*
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity
@Table(name = "spex_categories")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@SolrDocument(collection = "spexCategory")
data class SpexCategory (
    @Size(max = 255)
    @Column(name = "name", length = 255, nullable = false)
    var name: String,
    @Size(max = 4)
    @Pattern(regexp = "^(19|20|21)\\d{2}$")
    @Column(name = "first_year", length = 4, nullable = false)
    var firstYear: String,
    @Lob
    @Column(name = "logo")
    var logo: ByteArray,
    @Column(name = "logo_content_type")
    var logoContentType: String
): Base() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpexCategory

        if (id != other.id) return false
        if (name != other.name) return false
        if (firstYear != other.firstYear) return false
        if (logoContentType != other.logoContentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + firstYear.hashCode()
        result = 31 * result + logoContentType.hashCode()
        return result
    }
}
