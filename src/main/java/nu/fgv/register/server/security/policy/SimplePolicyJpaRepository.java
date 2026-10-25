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

package nu.fgv.register.server.security.policy;

import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The implementation behind every {@link PolicyJpaRepository}, set as the repository base class.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
public class SimplePolicyJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements PolicyJpaRepository<T, ID> {

    public SimplePolicyJpaRepository(final JpaEntityInformation<T, ?> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
    }

    public SimplePolicyJpaRepository(final Class<T> domainClass, final EntityManager entityManager) {
        this(JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager), entityManager);
    }

    @Override
    public List<T> findAll(final Permission permission) {
        return findAll(permitted(null, permission));
    }

    @Override
    public List<T> findAll(final Sort sort, final Permission permission) {
        return findAll(permitted(null, permission), sort);
    }

    @Override
    public List<T> findAll(@Nullable final Specification<T> spec, final Sort sort, final Permission permission) {
        return findAll(permitted(spec, permission), sort);
    }

    @Override
    public Page<T> findAll(final Pageable pageable, final Permission permission) {
        return findAll(permitted(null, permission), pageable);
    }

    @Override
    public List<T> findAll(@Nullable final Specification<T> spec, final Permission permission) {
        return findAll(permitted(spec, permission));
    }

    @Override
    public Page<T> findAll(@Nullable final Specification<T> spec, final Pageable pageable, final Permission permission) {
        return findAll(permitted(spec, permission), pageable);
    }

    @Override
    public <S extends T, R> R findBy(final Specification<T> spec,
                                     final Permission permission,
                                     final Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return findBy(permitted(spec, permission), queryFunction);
    }

    @Override
    public Stream<T> streamAll(final Permission permission) {
        return streamAll(null, Sort.unsorted(), permission);
    }

    @Override
    public Stream<T> streamAll(final Sort sort, final Permission permission) {
        return streamAll(null, sort, permission);
    }

    @Override
    public Stream<T> streamAll(@Nullable final Specification<T> spec, final Sort sort, final Permission permission) {
        return getQuery(permitted(spec, permission), sort).getResultStream();
    }

    /**
     * The policy's restriction for the current caller, and the given one when there is one.
     *
     * @param spec       the query's own restriction, or null
     * @param permission the permission asked for
     * @return the combined restriction
     * @throws IllegalStateException for an anonymous caller, rather than return what anonymous may see
     */
    protected Specification<T> permitted(@Nullable final Specification<T> spec, final Permission permission) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Permission filtering not possible for anonymous user");
        }

        final Specification<T> policy = AccessPolicies.get().permitted(getDomainClass(), permission);

        return spec == null ? policy : policy.and(spec);
    }
}
