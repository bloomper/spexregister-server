package nu.fgv.register.server.spex.category;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static nu.fgv.register.server.spex.category.SpexCategorySpecification.hasIds;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexCategoryService {

    private final SpexCategoryRepository repository;
    private final PermissionService permissionService;

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<SpexCategoryDto> findAll(final Sort sort) {
        return repository
                .findAll(sort, BasePermission.READ)
                .stream()
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Page<SpexCategoryDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<SpexCategory>builder().build(FilterParser.parse(filter), SpexCategorySpecification::new), pageable, BasePermission.READ)
                        .map(SPEX_CATEGORY_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Optional<SpexCategoryDto> findById(final Long id) {
        return repository
                .findById0(id)
                .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<SpexCategoryDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort, BasePermission.READ)
                .stream()
                .map(SPEX_CATEGORY_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public SpexCategoryDto create(final SpexCategoryCreateDto dto) {
        return Optional.of(SPEX_CATEGORY_MAPPER.toModel(dto))
                .map(repository::save)
                .map(spexCategory -> {
                    final ObjectIdentity oid = toObjectIdentity(SpexCategory.class, spexCategory.getId());

                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.DELETE, ROLE_ADMIN_SID);

                    return SPEX_CATEGORY_MAPPER.toDto(spexCategory);
                })
                .orElse(null);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<SpexCategoryDto> update(final SpexCategoryUpdateDto dto) {
        return partialUpdate(dto);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<SpexCategoryDto> partialUpdate(final SpexCategoryUpdateDto dto) {
        return repository
                .findById0(dto.getId())
                .map(category -> {
                    SPEX_CATEGORY_MAPPER.toPartialModel(dto, category);
                    return category;
                })
                .map(repository::save)
                .map(SPEX_CATEGORY_MAPPER::toDto);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public void deleteById(final Long id) {
        repository.deleteById(id);
        permissionService.deleteAcl(toObjectIdentity(SpexCategory.class, id));
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<SpexCategoryDto> saveLogo(final Long spexId, final byte[] logo, final String contentType) {
        return repository
                .findById0(spexId)
                .map(category -> {
                    category.setLogo(logo);
                    category.setLogoContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(logo));
                    repository.save(category);
                    return SPEX_CATEGORY_MAPPER.toDto(category);
                });
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<SpexCategoryDto> deleteLogo(final Long spexId) {
        return repository
                .findById0(spexId)
                .map(category -> {
                    category.setLogo(null);
                    category.setLogoContentType(null);
                    repository.save(category);
                    return SPEX_CATEGORY_MAPPER.toDto(category);
                });
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Optional<Pair<byte[], String>> getLogo(final Long spexId) {
        return repository
                .findById0(spexId)
                .filter(category -> category.getLogo() != null && hasText(category.getLogoContentType()))
                .map(category -> Pair.of(category.getLogo(), category.getLogoContentType()));
    }
}
