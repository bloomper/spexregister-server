package nu.fgv.register.server.domain

import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.elasticsearch.annotations.Document

@Entity
@Table(name = "spex_details")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "spexdetails")
data class SpexDetails(
    @get: NotNull
    @get: Size(max = 255)
    @Column(name = "title", length = 255, nullable = false)
    var title: String? = null,
    @Lob
    @Column(name = "poster")
    var poster: ByteArray? = null,
    @Column(name = "poster_content_type")
    var posterContentType: String? = null
) : Base() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpexDetails) return false
        if (other.id == null || id == null) return false

        return id == other.id
    }

    override fun hashCode() = 31
}
