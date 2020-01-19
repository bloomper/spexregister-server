package nu.fgv.register.server.model

import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class Base {
    @Id
    @GeneratedValue
    @Column(name = "id")
    @Field(type = FieldType.Keyword)
    open var id: String? = null
}
