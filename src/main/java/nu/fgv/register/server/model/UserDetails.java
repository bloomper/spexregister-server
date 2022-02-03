package nu.fgv.register.server.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "user_details")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class UserDetails implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Setter(AccessLevel.PROTECTED)
    private Long id;

    @OneToOne(optional = false)
    @NotNull
    @MapsId
    @JoinColumn(unique = true)
    private User user;

    @OneToOne
    @JoinColumn(unique = true)
    private Spexare spexare;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDetails userDetails = (UserDetails) o;
        if (userDetails.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), userDetails.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

}
