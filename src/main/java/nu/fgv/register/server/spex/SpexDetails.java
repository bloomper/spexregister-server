package nu.fgv.register.server.spex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "spex_details")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class SpexDetails extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{spex.title.notBlank}")
    @Size(max = 255, message = "{spex.title.size}")
    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "poster")
    @Basic(fetch = FetchType.LAZY)
    @ToString.Exclude
    private byte[] poster;

    @Column(name = "poster_content_type")
    private String posterContentType;

    @ManyToOne
    private SpexCategory category;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SpexDetails spexDetails = (SpexDetails) o;
        if (spexDetails.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), spexDetails.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }

}
