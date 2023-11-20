package nu.fgv.register.server.acl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "acl_object_identity", uniqueConstraints = {
        @UniqueConstraint(name = "UC_OBJECT_ID_CLASS_OBJECT_ID_IDENTITY", columnNames = {"object_id_class", "object_id_identity"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public final class AclObjectIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "object_id_class", referencedColumnName = "id", nullable = false)
    private AclClass objectIdClass;

    @Column(name = "object_id_identity", nullable = false)
    private Long objectIdIdentity;

    @ManyToOne
    @JoinColumn(name = "parent_object", referencedColumnName = "id")
    private AclObjectIdentity parentObject;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_sid", referencedColumnName = "id", nullable = false)
    private AclSid ownerSid;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AclObjectIdentity aclObjectIdentity = (AclObjectIdentity) o;
        if (aclObjectIdentity.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), aclObjectIdentity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}
