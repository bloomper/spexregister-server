package nu.fgv.register.server.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "task_category")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class TaskCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "has_actor")
    private Boolean hasActor;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TaskCategory taskCategory = (TaskCategory) o;
        if (taskCategory.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), taskCategory.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getClass().hashCode());
    }
}
