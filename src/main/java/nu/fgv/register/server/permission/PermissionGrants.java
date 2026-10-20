/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.permission;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import nu.fgv.register.server.security.policy.Caller;
import nu.fgv.register.server.security.policy.Permission;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.Activity_;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity_;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.User_;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
public class PermissionGrants {

    private static final String REQUEST_CACHE_KEY = PermissionGrants.class.getName() + ".grants";

    private final EntityManager entityManager;
    private final Clock clock;

    @Autowired
    public PermissionGrants(final EntityManager entityManager) {
        this(entityManager, Clock.systemDefaultZone());
    }

    PermissionGrants(final EntityManager entityManager, final Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    public Specification<Spexare> coveringSpexare(final Caller caller, final Permission permission) {
        return (root, query, criteriaBuilder) -> {
            if (!holdsAny(caller, permission)) {
                return criteriaBuilder.disjunction();
            }

            final Subquery<Long> grants = query.subquery(Long.class);
            final Root<PermissionGrant> grant = grants.from(PermissionGrant.class);

            final Subquery<Long> participations = query.subquery(Long.class);
            final Root<SpexActivity> spexActivity = participations.from(SpexActivity.class);
            final Join<SpexActivity, Activity> activity = spexActivity.join(SpexActivity_.activity);

            participations.select(spexActivity.get(SpexActivity_.id))
                    .where(criteriaBuilder.equal(activity.get(Activity_.spexare), root),
                            criteriaBuilder.equal(spexActivity.get(SpexActivity_.spex), grant.get(PermissionGrant_.spex)));

            grants.select(grant.get(PermissionGrant_.id))
                    .where(criteriaBuilder.and(
                            applies(criteriaBuilder, grant, caller, permission),
                            criteriaBuilder.or(
                                    criteriaBuilder.and(
                                            criteriaBuilder.equal(grant.get(PermissionGrant_.scope), PermissionGrantScope.SPEXARE),
                                            criteriaBuilder.equal(grant.get(PermissionGrant_.spexare), root)),
                                    criteriaBuilder.and(
                                            criteriaBuilder.equal(grant.get(PermissionGrant_.scope), PermissionGrantScope.SPEX_PARTICIPANTS),
                                            criteriaBuilder.exists(participations)))));

            return criteriaBuilder.exists(grants);
        };
    }

    public Specification<Spex> coveringSpex(final Caller caller, final Permission permission) {
        return (root, query, criteriaBuilder) -> {
            if (!holdsAny(caller, permission)) {
                return criteriaBuilder.disjunction();
            }

            final Subquery<Long> grants = query.subquery(Long.class);
            final Root<PermissionGrant> grant = grants.from(PermissionGrant.class);

            grants.select(grant.get(PermissionGrant_.id))
                    .where(criteriaBuilder.and(
                            applies(criteriaBuilder, grant, caller, permission),
                            criteriaBuilder.equal(grant.get(PermissionGrant_.scope), PermissionGrantScope.SPEX),
                            criteriaBuilder.equal(grant.get(PermissionGrant_.spex), root)));

            return criteriaBuilder.exists(grants);
        };
    }

    public boolean covers(final Caller caller, final Spexare spexare, final Permission permission) {
        if (!mayHoldGrants(caller) || spexare.getId() == null) {
            return false;
        }

        final List<PermissionGrant> grants = grantsOf(caller, permission);

        if (grants.isEmpty()) {
            return false;
        }

        Set<Long> spexIds = null;

        for (final PermissionGrant grant : grants) {
            if (grant.getScope() == PermissionGrantScope.SPEXARE
                    && grant.getSpexare() != null && spexare.getId().equals(grant.getSpexare().getId())) {
                return true;
            }
            if (grant.getScope() == PermissionGrantScope.SPEX_PARTICIPANTS && grant.getSpex() != null) {
                if (spexIds == null) {
                    spexIds = participatedSpexIds(spexare.getId());
                }
                if (spexIds.contains(grant.getSpex().getId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean covers(final Caller caller, final Spex spex, final Permission permission) {
        if (!mayHoldGrants(caller) || spex.getId() == null) {
            return false;
        }

        return grantsOf(caller, permission).stream()
                .anyMatch(grant -> grant.getScope() == PermissionGrantScope.SPEX
                        && grant.getSpex() != null && spex.getId().equals(grant.getSpex().getId()));
    }

    public Set<Long> readableSpexareIds(final Caller caller) {
        if (!holdsAny(caller, Permission.READ)) {
            return Set.of();
        }

        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        final Root<Spexare> root = query.from(Spexare.class);

        query.select(root.get("id")).where(coveringSpexare(caller, Permission.READ).toPredicate(root, query, criteriaBuilder));

        return new HashSet<>(entityManager.createQuery(query).getResultList());
    }

    private boolean holdsAny(final Caller caller, final Permission permission) {
        return mayHoldGrants(caller) && !grantsOf(caller, permission).isEmpty();
    }

    private static boolean mayHoldGrants(final Caller caller) {
        return caller.subject() != null || !caller.roles().isEmpty();
    }

    private Predicate applies(final CriteriaBuilder criteriaBuilder,
                              final Root<PermissionGrant> grant,
                              final Caller caller,
                              final Permission permission) {
        final LocalDate today = LocalDate.now(clock);
        final List<Predicate> grantees = new ArrayList<>();

        if (caller.subject() != null) {
            final Join<PermissionGrant, User> user = grant.join(PermissionGrant_.user, JoinType.LEFT);
            grantees.add(criteriaBuilder.equal(user.get(User_.externalId), caller.subject()));
        }
        if (!caller.roles().isEmpty()) {
            grantees.add(grant.get(PermissionGrant_.role).in(caller.roles()));
        }

        final Path<LocalDate> validUntil = grant.get(PermissionGrant_.validUntil);

        return criteriaBuilder.and(
                criteriaBuilder.or(grantees.toArray(Predicate[]::new)),
                criteriaBuilder.or(criteriaBuilder.isNull(validUntil), criteriaBuilder.greaterThanOrEqualTo(validUntil, today)),
                grant.get(PermissionGrant_.permission).in(permissionsMeeting(permission)));
    }

    private static List<Permission> permissionsMeeting(final Permission permission) {
        return Arrays.stream(Permission.values()).filter(permission::isMetBy).toList();
    }

    private List<PermissionGrant> grantsOf(final Caller caller, final Permission permission) {
        return allGrantsOf(caller).stream()
                .filter(grant -> permission.isMetBy(grant.getPermission()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<PermissionGrant> allGrantsOf(final Caller caller) {
        final RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        final String key = REQUEST_CACHE_KEY + "." + Objects.hashCode(caller);

        if (attributes != null && attributes.getAttribute(key, RequestAttributes.SCOPE_REQUEST) instanceof List<?> cached) {
            return (List<PermissionGrant>) cached;
        }

        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<PermissionGrant> query = criteriaBuilder.createQuery(PermissionGrant.class);
        final Root<PermissionGrant> grant = query.from(PermissionGrant.class);

        query.select(grant).where(applies(criteriaBuilder, grant, caller, Permission.READ));

        final List<PermissionGrant> grants = entityManager.createQuery(query).getResultList();

        if (attributes != null) {
            attributes.setAttribute(key, grants, RequestAttributes.SCOPE_REQUEST);
        }

        return grants;
    }

    private Set<Long> participatedSpexIds(final Long spexareId) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        final Root<SpexActivity> spexActivity = query.from(SpexActivity.class);
        final Join<SpexActivity, Activity> activity = spexActivity.join(SpexActivity_.activity);
        final Join<SpexActivity, Spex> spex = spexActivity.join(SpexActivity_.spex);

        query.select(spex.get("id")).where(criteriaBuilder.equal(activity.get(Activity_.spexare).get("id"), spexareId));

        return new HashSet<>(entityManager.createQuery(query).getResultList());
    }
}
