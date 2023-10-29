package nu.fgv.register.server.news;

import lombok.Getter;
import nu.fgv.register.server.util.filter.BaseSpecification;
import nu.fgv.register.server.util.filter.FilterCriteria;

@Getter
public class NewsSpecification extends BaseSpecification<News> {

    public NewsSpecification(final FilterCriteria criteria) {
        super(criteria);
    }

}
