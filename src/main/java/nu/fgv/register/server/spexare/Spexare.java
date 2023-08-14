package nu.fgv.register.server.spexare;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import nu.fgv.register.server.event.JpaEntityListener;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.address.Address;
import nu.fgv.register.server.spexare.consent.Consent;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.toggle.Toggle;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.util.AbstractAuditable;
import nu.fgv.register.server.util.CryptoConverter;
import nu.fgv.register.server.util.Luhn;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "spexare")
@EntityListeners(JpaEntityListener.class)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Indexed(index = "spexare")
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class Spexare extends AbstractAuditable implements Serializable {

    public static final String SOCIAL_SECURITY_NUMBER_PATTERN = "(19|20)([0-9]{2})((0[1-9])|(10|11|12))(([0][1-9])|([1-2][0-9])|(3[0-1]))(-(\\d{3})(\\d))?";

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "{spexare.firstName.notEmpty}")
    @Size(max = 255, message = "{spexare.firstName.size}")
    @Column(name = "first_name", nullable = false)
    @KeywordField(searchable = Searchable.YES, sortable = Sortable.YES)
    private String firstName;

    @NotEmpty(message = "{spexare.lastName.notEmpty}")
    @Size(max = 255, message = "{spexare.lastName.size}")
    @Column(name = "last_name", nullable = false)
    @KeywordField(searchable = Searchable.YES, sortable = Sortable.YES)
    private String lastName;

    @Size(max = 255, message = "{spexare.nickName.size}")
    @Column(name = "nick_name")
    @KeywordField(searchable = Searchable.YES, sortable = Sortable.YES)
    private String nickName;

    @Pattern(regexp = SOCIAL_SECURITY_NUMBER_PATTERN, message = "{spexare.socialSecurityNumber.regexp}")
    @Luhn(regexp = SOCIAL_SECURITY_NUMBER_PATTERN, existenceGroup = 10, inputGroups = {2, 3, 6, 11}, controlGroup = 12, message = "{spexare.socialSecurityNumber.luhn}")
    @Column(name = "social_security_number")
    @Convert(converter = CryptoConverter.class)
    @GenericField(searchable = Searchable.YES)
    private String socialSecurityNumber;

    @Size(max = 255, message = "{spexare.graduation.size}")
    @Column(name = "graduation")
    @GenericField(searchable = Searchable.YES)
    private String graduation;

    @Lob
    @Column(name = "comment")
    @FullTextField(searchable = Searchable.YES)
    private String comment;

    @Lob
    @Column(name = "image", columnDefinition = "MEDIUMBLOB")
    private byte[] image;

    @Column(name = "image_content_type")
    private String imageContentType;

    @ManyToOne
    private Spexare partner;

    @OneToOne(mappedBy = "spexare")
    private User user;

    @OneToMany(mappedBy = "spexare", cascade = {CascadeType.REMOVE}, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ToString.Exclude
    @IndexedEmbedded
    private Set<Activity> activities = new HashSet<>();

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "tagging",
            joinColumns = @JoinColumn(name = "spexare_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id"))
    @ToString.Exclude
    @IndexedEmbedded
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "spexare", cascade = {CascadeType.REMOVE}, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ToString.Exclude
    @IndexedEmbedded
    private List<Address> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "spexare", cascade = {CascadeType.REMOVE}, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ToString.Exclude
    @IndexedEmbedded
    private List<Membership> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "spexare", cascade = {CascadeType.REMOVE}, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ToString.Exclude
    @IndexedEmbedded
    private List<Consent> consents = new ArrayList<>();

    @OneToMany(mappedBy = "spexare", cascade = {CascadeType.REMOVE}, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ToString.Exclude
    @IndexedEmbedded
    private List<Toggle> toggles = new ArrayList<>();

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
