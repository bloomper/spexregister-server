package nu.fgv.register.server.settings;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import nu.fgv.register.server.util.AbstractAuditable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Entity
@Table(name = "type")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class Type extends AbstractAuditable implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Size(max = 255, message = "{type.value.size}")
    @Column(name = "id")
    @GenericField(searchable = Searchable.NO)
    private String id;

    @org.hibernate.annotations.Type(JsonType.class)
    @Column(name = "labels", columnDefinition = "json")
    @GenericField(searchable = Searchable.NO)
    private Map<String, String> labels;

    @NotNull(message = "{type.type.notEmpty}")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @GenericField(searchable = Searchable.NO)
    private TypeType type;

}
