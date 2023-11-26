package nu.fgv.register.server.acl;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserClaim;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SimpleAclJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements AclJpaRepository<T, ID> {

    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager entityManager;

    public SimpleAclJpaRepository(final JpaEntityInformation<T, ?> entityInformation, final EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    public SimpleAclJpaRepository(final Class<T> domainClass, final EntityManager entityManager) {
        this(JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager), entityManager);
    }

    private static long executeCountQuery(final TypedQuery<Long> query) {
        Assert.notNull(query, "TypedQuery must not be null!");
        List<Long> totals = query.getResultList();

        return totals.stream().mapToLong(total -> null == total ? 0 : total).sum();
    }

    @Override
    public List<T> findAll(final Permission permission) {
        return findAll((Specification) null, permission);
    }

    @Override
    public List<T> findAll(final Sort sort, final Permission permission) {
        return findAll(null, sort, permission);
    }

    @Override
    public List<T> findAll(final Specification<T> spec, final Sort sort, final Permission permission) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (null == authentication || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Permission filtering not possible for anonymous user");
        }

        final PrincipalSid sid = new PrincipalSid(getCurrentUserClaim());

        return getQuery(spec, sort, sid, permission).getResultList();
    }

    @Override
    public Page<T> findAll(final Pageable pageable, final Permission permission) {
        return findAll(null, pageable, permission);
    }

    @Override
    public List<T> findAll(final Specification<T> spec, final Permission permission) {
        return findAll(spec, Sort.unsorted(), permission);
    }

    @Override
    public Page<T> findAll(final Specification<T> spec,
                           final Pageable pageable,
                           final Permission permission) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (null == authentication || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Permission filtering not possible for anonymous user");
        }

        final PrincipalSid sid = new PrincipalSid(getCurrentUserClaim());
        final TypedQuery<T> query = getQuery(spec, pageable, sid, permission);

        return pageable.isUnpaged() ? new PageImpl<>(query.getResultList()) :
                readPage(query, getDomainClass(), pageable, spec, sid, permission);
    }

    protected <S extends T> Page<S> readPage(final TypedQuery<S> query,
                                             final Class<S> domainClass,
                                             final Pageable pageable,
                                             @Nullable final Specification<S> spec,
                                             final PrincipalSid sid,
                                             final Permission permission) {
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        return PageableExecutionUtils.getPage(query.getResultList(), pageable,
                () -> executeCountQuery(getCountQuery(spec, domainClass, sid, permission)));
    }

    protected TypedQuery<T> getQuery(@Nullable final Specification<T> spec,
                                     final Pageable pageable,
                                     final PrincipalSid sid,
                                     final Permission permission) {
        final Sort sort = pageable.isPaged() ? pageable.getSort() : Sort.unsorted();

        return getQuery(spec, getDomainClass(), sort, sid, permission);
    }

    protected TypedQuery<T> getQuery(@Nullable final Specification<T> spec,
                                     final Sort sort,
                                     final PrincipalSid sid,
                                     final Permission permission) {
        return getQuery(spec, getDomainClass(), sort, sid, permission);
    }

    protected <S extends T> TypedQuery<S> getQuery(@Nullable final Specification<S> spec,
                                                   final Class<S> domainClass,
                                                   final Sort sort,
                                                   final PrincipalSid sid,
                                                   final Permission permission) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<S> criteriaQuery = criteriaBuilder.createQuery(domainClass);
        final Root<S> root = applySpecificationToCriteria(spec, domainClass, criteriaQuery, sid, permission);

        criteriaQuery.select(root);

        if (sort.isSorted()) {
            criteriaQuery.orderBy(QueryUtils.toOrders(sort, root, criteriaBuilder));
        }

        return entityManager.createQuery(criteriaQuery);
    }

    protected <S extends T> TypedQuery<Long> getCountQuery(@Nullable final Specification<S> spec,
                                                           final Class<S> domainClass,
                                                           final PrincipalSid sid,
                                                           final Permission permission) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        final Root<S> root = applySpecificationToCriteria(spec, domainClass, criteriaQuery, sid, permission);

        if (criteriaQuery.isDistinct()) {
            criteriaQuery.select(criteriaBuilder.countDistinct(root));
        } else {
            criteriaQuery.select(criteriaBuilder.count(root));
        }

        criteriaQuery.orderBy(Collections.emptyList());

        return entityManager.createQuery(criteriaQuery);
    }

    private <S, U extends T> Root<U> applySpecificationToCriteria(@Nullable final Specification<U> spec,
                                                                  final Class<U> domainClass,
                                                                  final CriteriaQuery<S> query,
                                                                  final PrincipalSid sid,
                                                                  final Permission permission) {
        Assert.notNull(domainClass, "Domain class must not be null!");
        Assert.notNull(query, "CriteriaQuery must not be null!");

        final Root<U> root = query.from(domainClass);

        if (null == spec) {
            query.where(filterPermitted(root, query, domainClass, sid, permission));
        } else {
            final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            final Predicate predicate = spec.toPredicate(root, query, criteriaBuilder);

            if (null != predicate) {
                query.where(criteriaBuilder.and(predicate, filterPermitted(root, query, domainClass, sid, permission)));
            } else {
                query.where(filterPermitted(root, query, domainClass, sid, permission));
            }

        }

        return root;
    }

    private <S, U extends T> Predicate filterPermitted(final Root<U> root,
                                                       final CriteriaQuery<S> query,
                                                       final Class<U> domainClass,
                                                       final PrincipalSid sid,
                                                       final Permission permission) {
        return root.<Long>get(entityInformation.getRequiredIdAttribute().getName())
                .in(selectPermittedIds(query, domainClass, sid, permission));
    }

    private <S> Subquery<Long> selectPermittedIds(final CriteriaQuery<S> query,
                                                  final Class<?> targetType,
                                                  final PrincipalSid sid,
                                                  final Permission permission) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final Subquery<Long> aclEntryQuery = query.subquery(Long.class);
        final Root<AclEntry> root = aclEntryQuery.from(AclEntry.class);
        final Join<AclEntry, AclObjectIdentity> aclObjectIdentityJoin = root.join(AclEntry_.ACL_OBJECT_IDENTITY);
        final Join<AclEntry, AclObjectIdentity> aclSidJoin = root.join(AclEntry_.ACL_SID);

        return aclEntryQuery.select(aclObjectIdentityJoin.get(AclObjectIdentity_.OBJECT_ID_IDENTITY))
                .where(criteriaBuilder.and(
                        aclObjectIdentityJoin.<Long>get(AclObjectIdentity_.ID).in(selectAclObjectIdentityId(aclEntryQuery, targetType)),
                        criteriaBuilder.equal(aclSidJoin.<Long>get(AclSid_.ID), selectAclSidId(aclEntryQuery, sid)),
                        criteriaBuilder.equal(root.<Integer>get(AclEntry_.MASK), permission.getMask())));
    }

    private <S> Subquery<Long> selectAclObjectIdentityId(final Subquery<S> query,
                                                         final Class<?> targetType) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final Subquery<Long> aclObjectIdentityQuery = query.subquery(Long.class);
        final Root<AclObjectIdentity> root = aclObjectIdentityQuery.from(AclObjectIdentity.class);
        final Join<AclObjectIdentity, AclClass> aclClassJoin = root.join(AclObjectIdentity_.OBJECT_ID_CLASS);

        return aclObjectIdentityQuery.select(root.get(AclObjectIdentity_.ID))
                .where(criteriaBuilder.equal(aclClassJoin.get(AclClass_.ID),
                        selectAclClassId(aclObjectIdentityQuery, targetType)));
    }

    private <S> Subquery<Long> selectAclSidId(final Subquery<S> query, final PrincipalSid sid) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final Subquery<Long> aclSidQuery = query.subquery(Long.class);
        final Root<AclSid> root = aclSidQuery.from(AclSid.class);

        return aclSidQuery.select(root.get(AclSid_.ID))
                .where(criteriaBuilder.equal(root.<String>get(AclSid_.SID), sid.getPrincipal()));
    }

    private <S> Subquery<Long> selectAclClassId(final Subquery<S> query, final Class<?> targetType) {
        final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        final Subquery<Long> aclClassQuery = query.subquery(Long.class);
        final Root<AclClass> root = aclClassQuery.from(AclClass.class);

        return aclClassQuery.select(root.get(AclClass_.ID))
                .where(criteriaBuilder.equal(root.<String>get(AclClass_.CLASS_NAME), targetType.getSimpleName()));
    }
}
