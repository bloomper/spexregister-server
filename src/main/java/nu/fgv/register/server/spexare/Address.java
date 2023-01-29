package nu.fgv.register.server.spexare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.user.UserDetails;
import nu.fgv.register.server.util.AbstractAuditable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "address")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class Address extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255, message = "{address.streetAddress.size}")
    @Column(name = "street_address")
    private String streetAddress;

    @Size(max = 255, message = "{address.postalCode.size}")
    @Column(name = "postal_code")
    private String postalCode;

    @Size(max = 255, message = "{address.city.size}")
    @Column(name = "city")
    private String city;

    @Size(max = 255, message = "{address.country.size}")
    @Column(name = "country")
    private String country;

    @Size(max = 255, message = "{address.phone.size}")
    @Column(name = "phone")
    private String phone;

    @Size(max = 255, message = "{address.phoneMobile.size}")
    @Column(name = "phone_mobile")
    private String phoneMobile;

    @Size(max = 255, message = "{address.emailAddress.size}")
    @Email(message = "{address.emailAddress.valid}")
    @Column(name = "email_address")
    private String emailAddress;

    @NotNull(message = "{address.type.notEmpty}")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType type;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Address address = (Address) o;
        if (address.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), address.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}
