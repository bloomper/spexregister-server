package nu.fgv.register.server.util;

import lombok.Getter;
import lombok.Setter;
import nu.fgv.register.server.util.export.model.ExcelCell;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AbstractAuditable {

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    @ExcelCell(header = "Created by")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @ExcelCell(header = "Created at")
    private long createdAt = Instant.now().toEpochMilli();

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    @ExcelCell(header = "Last modified by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_at")
    @ExcelCell(header = "Last modified at")
    private long lastModifiedAt = Instant.now().toEpochMilli();

}
