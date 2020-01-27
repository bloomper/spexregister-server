package nu.fgv.register.server.model

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.EntityListeners
import javax.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AuditedBase(
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    var createdAt: LocalDateTime? = null,

    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    @CreatedBy
    var createdBy: String? = null,

    @Column(name = "last_modified_at")
    @LastModifiedDate
    var lastModifiedAt: LocalDateTime? = null,

    @Column(name = "last_modified_by", length = 50)
    @LastModifiedBy
    var lastModifiedBy: String? = null
) : Base()
