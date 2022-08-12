package nu.fgv.register.server.spex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "spex")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class Spex extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{spex.year.notBlank}")
    @Size(max = 4, message = "{spex.year.size}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spex.year.regexp}")
    @Column(name = "year", length = 4, nullable = false)
    private String year;

    @ManyToOne
    private Spex parent;

    @ManyToOne(optional = false)
    private SpexDetails details;

    public boolean isRevival() {
        return parent != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Spex spex = (Spex) o;
        if (spex.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), spex.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}
