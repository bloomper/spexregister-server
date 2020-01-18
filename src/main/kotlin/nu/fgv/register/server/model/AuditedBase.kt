package nu.fgv.register.server.model

import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AuditedBase(
        @Column(name = "created_at", nullable = false, updatable = false)
        @CreatedDate
        open val createdAt: LocalDateTime? = null,

        @Column(name = "created_by", updatable = false)
        @CreatedBy
        open val createdBy: String? = null,

        @Column(name = "last_modified_at")
        @LastModifiedDate
        open val lastModifiedAt: LocalDateTime? = null,

        @Column(name = "last_modified_by")
        @LastModifiedBy
        open val lastModifiedBy: String? = null
): Base()
