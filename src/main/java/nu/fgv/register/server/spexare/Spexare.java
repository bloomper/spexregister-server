package nu.fgv.register.server.spexare;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.user.UserDetails;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "spexare")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class Spexare extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotNull
    @Size(max = 255)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Size(max = 255)
    @Column(name = "nick_name")
    private String nickName;

    @Size(max = 255)
    @Column(name = "street_address")
    private String streetAddress;

    @Size(max = 255)
    @Column(name = "postal_code")
    private String postalCode;

    @Size(max = 255)
    @Column(name = "postal_address")
    private String postalAddress;

    @Size(max = 255)
    @Column(name = "country")
    private String country;

    @Size(max = 255)
    @Column(name = "phone_home")
    private String phoneHome;

    @Size(max = 255)
    @Column(name = "phone_work")
    private String phoneWork;

    @Size(max = 255)
    @Column(name = "phone_mobile")
    private String phoneMobile;

    @Size(max = 255)
    @Column(name = "phone_other")
    private String phoneOther;

    @Size(max = 255)
    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Size(max = 4)
    @Pattern(regexp = "^\\d{4}$")
    @Column(name = "social_security_number", length = 4)
    private String socialSecurityNumber;

    @Column(name = "chalmers_student")
    private Boolean chalmersStudent;

    @Size(max = 255)
    @Column(name = "graduation")
    private String graduation;

    @Lob
    @Column(name = "comment")
    private String comment;

    @Column(name = "deceased")
    private Boolean deceased;

    @Column(name = "publish_approval")
    private Boolean publishApproval;

    @Column(name = "want_circulars")
    private Boolean wantCirculars;

    @Column(name = "want_email_circulars")
    private Boolean wantEmailCirculars;

    @Lob
    @Column(name = "image")
    private byte[] image;

    @Column(name = "image_content_type")
    private String imageContentType;

    @OneToMany(mappedBy = "spexare")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ToString.Exclude
    private Set<Activity> activities = new HashSet<>();

    @ManyToOne
    private Spexare spouse;

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "tagging",
               joinColumns = @JoinColumn(name = "spexare_id", referencedColumnName = "id"),
               inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id"))
    @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "spexare")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ToString.Exclude
    private List<Membership> memberships = new ArrayList<>();

    @OneToOne(mappedBy = "spexare")
    private UserDetails userDetails;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Spexare spexare = (Spexare) o;
        if (spexare.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), spexare.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}
