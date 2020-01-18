package nu.fgv.register.server.model

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.solr.core.mapping.SolrDocument
import javax.persistence.*
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Entity
@Table(name = "spex")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@SolrDocument(collection = "spex")
data class Spex (
        @Size(max = 4)
        @Pattern(regexp = "^(19|20|21)\\d{2}$")
        @Column(name = "year", length = 4, nullable = false)
        var year: String,
        @ManyToOne(optional = false)
        var category: SpexCategory,
        @ManyToOne
        var parent: Spex,
        @ManyToOne(optional = false)
        var details: SpexDetails
): Base()
