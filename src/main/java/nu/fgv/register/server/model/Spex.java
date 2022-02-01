package nu.fgv.register.server.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.elasticsearch.annotations.Document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "spex")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "spex")
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class Spex extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.PROTECTED)
    private Long id;

    @NotNull
    @Size(max = 4)
    @Pattern(regexp = "^(19|20|21)\\d{2}$")
    @Column(name = "year", length = 4, nullable = false)
    private String year;

    @ManyToOne(optional = false)
    @NotNull
    private SpexCategory category;

    @ManyToOne
    private Spex parent;

    @ManyToOne(optional = false)
    @NotNull
    private SpexDetails details;

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
        return Objects.hashCode(getId());
    }

}
