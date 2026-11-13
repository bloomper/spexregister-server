package nu.fgv.register.server.spex.character;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.util.AbstractAuditable;
import org.hibernate.envers.Audited;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A character of a spex, copied from Spexipedia by the sync: an original's, or one a revival added. Actors point at
 * characters, so that Spexipedia shows exactly who played whom.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Entity
@Table(name = "spex_character")
@Audited
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class SpexCharacter extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Spexipedia's id.
     */
    @Column(name = "uuid", nullable = false, updatable = false, unique = true, length = 36)
    private UUID uuid;

    @ManyToOne(optional = false)
    @ToString.Exclude
    private Spex spex;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "retired", nullable = false)
    private boolean retired;

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SpexCharacter other = (SpexCharacter) o;

        return other.getId() != null && getId() != null && Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass().hashCode());
    }
}
