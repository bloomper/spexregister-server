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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "spex_category")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class SpexCategory extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{spexCategory.name.notEmpty}")
    @Size(max = 255, message = "{spexCategory.name.maxSize}")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "{spexCategory.firstYear.notEmpty}")
    @Size(max = 4, message = "{spexCategory.firstYear.maxSize}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spexCategory.firstYear.regexp}")
    @Column(name = "first_year", length = 4, nullable = false)
    private String firstYear;

    @Lob
    @Column(name = "logo", columnDefinition = "MEDIUMBLOB")
    @Basic(fetch = FetchType.LAZY)
    @ToString.Exclude
    private byte[] logo;

    @Column(name = "logo_content_type")
    private String logoContentType;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SpexCategory spexCategory = (SpexCategory) o;
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
