package nu.fgv.register.server.spexare.membership;

import nu.fgv.register.server.util.search.StringTypeBinder;

public class MembershipYearBinder extends StringTypeBinder<Membership> {
    public MembershipYearBinder() {
        super(Membership.class, Membership::getType, Membership::getYear, "year", true);
    }
}