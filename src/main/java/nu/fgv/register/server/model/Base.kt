package nu.fgv.register.server.model

import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class Base {
    @Id
    @GeneratedValue
    @Column(name = "id")
    open var id: String? = null
}