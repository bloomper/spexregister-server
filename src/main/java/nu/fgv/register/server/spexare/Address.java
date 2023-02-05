package nu.fgv.register.server.spexare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.util.AbstractAuditable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

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
    @ManyToOne(optional = false)
    private Type type;

    @ManyToOne
    private Spexare spexare;

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
