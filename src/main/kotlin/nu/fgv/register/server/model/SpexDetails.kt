package nu.fgv.register.server.model

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.elasticsearch.annotations.Document
import javax.persistence.*
import javax.validation.constraints.Size

@Entity
@Table(name = "spex_details")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "spexdetails")
data class SpexDetails (
        @Size(max = 255)
        @Column(name = "title", length = 255, nullable = false)
        var title: String,
        @Lob
        @Column(name = "poster")
        var poster: ByteArray,
        @Column(name = "poster_content_type")
        var posterContentType: String
): Base() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SpexDetails

        if (id != other.id) return false
        if (title != other.title) return false
        if (posterContentType != other.posterContentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + title.hashCode()
        result = 31 * result + posterContentType.hashCode()
        return result
    }
}
