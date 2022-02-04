package nu.fgv.register.server.spex;

import lombok.AllArgsConstructor;
import lombok.Builder;
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
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "spex_category")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class SpexCategory extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Size(max = 4)
    @Pattern(regexp = "^(19|20|21)\\d{2}$")
    @Column(name = "first_year", length = 4, nullable = false)
    private String firstYear;

    @Lob
    @Column(name = "logo")
    private byte[] logo;

    @Column(name = "logo_content_type")
    private String logoContentType;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SpexCategory spexCategory = (SpexCategory) o;
        if (spexCategory.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), spexCategory.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}
