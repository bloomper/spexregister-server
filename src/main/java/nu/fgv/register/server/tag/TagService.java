package nu.fgv.register.server.tag;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;
import static nu.fgv.register.server.tag.TagSpecification.hasIds;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TagService {

    private final TagRepository repository;
    private final PermissionService permissionService;

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<TagDto> findAll(final Sort sort) {
        return repository
                .findAll(sort, BasePermission.READ)
                .stream()
                .map(TAG_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Page<TagDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Tag>builder().build(FilterParser.parse(filter), TagSpecification::new), pageable, BasePermission.READ)
                        .map(TAG_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(TAG_MAPPER::toDto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Optional<TagDto> findById(final Long id) {
        return repository
                .findById0(id)
                .map(TAG_MAPPER::toDto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<TagDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort, BasePermission.READ)
                .stream()
                .map(TAG_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public TagDto create(final TagCreateDto dto) {
        return Optional.of(TAG_MAPPER.toModel(dto))
                .map(model -> {
                    final Tag tag = repository.save(model);
                    final ObjectIdentity oid = toObjectIdentity(Tag.class, tag.getId());

                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.DELETE, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_USER_SID);

                    return TAG_MAPPER.toDto(tag);
                })
                .orElse(null);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public Optional<TagDto> update(final TagUpdateDto dto) {
        return partialUpdate(dto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public Optional<TagDto> partialUpdate(final TagUpdateDto dto) {
        return repository
                .findById0(dto.getId())
                .map(tag -> {
                    TAG_MAPPER.toPartialModel(dto, tag);
                    return tag;
                })
                .map(repository::save)
                .map(TAG_MAPPER::toDto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public void deleteById(final Long id) {
        repository.deleteById(id);
        permissionService.deleteAcl(toObjectIdentity(Tag.class, id));
    }

}
