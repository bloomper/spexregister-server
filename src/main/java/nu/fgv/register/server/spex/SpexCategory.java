package nu.fgv.register.server.spex;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.export.model.ExcelCell;
import nu.fgv.register.server.util.export.model.ExcelSheet;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
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
@ExcelSheet(name = "Spex categories")
public class SpexCategory extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ExcelCell(header = "Id")
    private Long id;

    @NotBlank(message = "{spexCategory.name.notBlank}")
    @Size(max = 255, message = "{spexCategory.name.maxSize}")
    @Column(name = "name", nullable = false)
    @ExcelCell(header = "Name")
    private String name;

    @NotBlank(message = "{spexCategory.firstYear.notBlank}")
    @Size(max = 4, message = "{spexCategory.firstYear.maxSize}")
    @Pattern(regexp = "^(19|20|21)\\d{2}$", message = "{spexCategory.firstYear.pattern}")
    @Column(name = "first_year", length = 4, nullable = false)
    @ExcelCell(header = "First year")
    private String firstYear;

    @Lob
    @Column(name = "logo")
    @Basic(fetch = FetchType.LAZY)
    @ToString.Exclude
    @ExcelCell.Exclude
    private byte[] logo;

    @Column(name = "logo_content_type")
    @ExcelCell.Exclude
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
