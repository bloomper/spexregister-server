package nu.fgv.register.server.settings;

import org.springframework.data.jpa.domain.Specification;

public class TypeSpecification {

    private TypeSpecification() {
    }

    public static Specification<Type> hasId(final String id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Type_.id), id);
    }

    public static Specification<Type> hasType(final TypeType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(Type_.type), type);
    }

}
