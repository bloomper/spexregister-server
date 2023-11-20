package nu.fgv.register.server.acl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Immutable;

import java.util.Objects;

@Entity
@Immutable
@Table(name = "acl_sid", uniqueConstraints = {
        @UniqueConstraint(name = "UC_SID_PRINCIPAL", columnNames = {"sid", "principal"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public final class AclSid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "principal", nullable = false)
    private boolean principal;

    @Column(name = "sid", nullable = false, length = 100)
    private String sid;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AclSid aclSid = (AclSid) o;
        if (aclSid.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), aclSid.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}
