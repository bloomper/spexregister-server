package nu.fgv.register.server.spexare;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import jakarta.validation.constraints.NotEmpty;
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

    @NotEmpty(message = "{spexare.firstName.notEmpty}")
    @Size(max = 255, message = "{spexare.firstName.size}")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotEmpty(message = "{spexare.lastName.notEmpty}")
    @Size(max = 255, message = "{spexare.lastName.size}")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Size(max = 255, message = "{spexare.nickName.size}")
    @Column(name = "nick_name")
    private String nickName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Size(max = 4, message = "{spexare.socialSecurityNumber.size}")
    @Pattern(regexp = "^\\d{4}$", message = "{spexare.socialSecurityNumber.size}")
    @Column(name = "social_security_number", length = 4)
    private String socialSecurityNumber;

    @Column(name = "chalmers_student")
    private Boolean chalmersStudent;

    @Size(max = 255, message = "{spexare.graduation.size}")
    @Column(name = "graduation")
    private String graduation;

    @Lob
    @Column(name = "comment")
    private String comment;

    @Column(name = "deceased")
    private Boolean deceased;

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
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "spexare")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ToString.Exclude
    private List<Membership> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "spexare")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ToString.Exclude
    private List<Consent> consents = new ArrayList<>();

    @OneToOne(mappedBy = "spexare")
    private UserDetails userDetails;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Spexare spexare = (Spexare) o;
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
