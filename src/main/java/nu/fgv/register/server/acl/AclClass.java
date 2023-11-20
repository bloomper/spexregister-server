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
@Table(name = "acl_class", uniqueConstraints = {
        @UniqueConstraint(name = "UC_CLASS", columnNames = {"class"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public final class AclClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class", nullable = false)
    private String className;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AclClass aclClass = (AclClass) o;
        if (aclClass.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), aclClass.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}
