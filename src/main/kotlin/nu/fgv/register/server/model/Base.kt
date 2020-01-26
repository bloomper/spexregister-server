package nu.fgv.register.server.model

import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.io.Serializable
import javax.persistence.*

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
