package nu.fgv.register.server.domain

import java.io.Serializable
import javax.persistence.*
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@MappedSuperclass
abstract class Base(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Field(type = FieldType.Keyword)
    var id: Long? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
