package nu.fgv.register.server.spexare.membership;

import nu.fgv.register.server.util.search.StringTypeBinder;

/**
 * Indexes a membership's association and year, for the {@code memberships} facet.
 */
public class MembershipYearBinder extends StringTypeBinder<Membership> {
    public MembershipYearBinder() {
        super(Membership.class, Membership::getType, Membership::getYear, "year", true);
    }
}