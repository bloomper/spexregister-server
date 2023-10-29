package nu.fgv.register.server.user;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;

@Getter
public class UserSpecification extends BaseSpecification<User> {

    public UserSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

}
