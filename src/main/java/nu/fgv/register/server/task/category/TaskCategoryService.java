package nu.fgv.register.server.task.category;

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

import static nu.fgv.register.server.task.category.TaskCategoryMapper.TASK_CATEGORY_MAPPER;
import static nu.fgv.register.server.task.category.TaskCategorySpecification.hasIds;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaskCategoryService {

    private final TaskCategoryRepository repository;
    private final PermissionService permissionService;

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<TaskCategoryDto> findAll(final Sort sort) {
        return repository
                .findAll(sort, BasePermission.READ)
                .stream()
                .map(TASK_CATEGORY_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Page<TaskCategoryDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<TaskCategory>builder().build(FilterParser.parse(filter), TaskCategorySpecification::new), pageable, BasePermission.READ)
                        .map(TASK_CATEGORY_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(TASK_CATEGORY_MAPPER::toDto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Optional<TaskCategoryDto> findById(final Long id) {
        return repository
                .findById0(id)
                .map(TASK_CATEGORY_MAPPER::toDto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<TaskCategoryDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort, BasePermission.READ)
                .stream()
                .map(TASK_CATEGORY_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public TaskCategoryDto create(final TaskCategoryCreateDto dto) {
        return Optional.of(TASK_CATEGORY_MAPPER.toModel(dto))
                .map(repository::save)
                .map(category -> {
                    final ObjectIdentity oid = toObjectIdentity(TaskCategory.class, category.getId());

                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID, ROLE_USER_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.DELETE, ROLE_ADMIN_SID);

                    return TASK_CATEGORY_MAPPER.toDto(category);
                })
                .orElse(null);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<TaskCategoryDto> update(final TaskCategoryUpdateDto dto) {
        return partialUpdate(dto);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public Optional<TaskCategoryDto> partialUpdate(final TaskCategoryUpdateDto dto) {
        return repository
                .findById0(dto.getId())
                .map(category -> {
                    TASK_CATEGORY_MAPPER.toPartialModel(dto, category);
                    return category;
                })
                .map(repository::save)
                .map(TASK_CATEGORY_MAPPER::toDto);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public void deleteById(final Long id) {
        repository.deleteById(id);
        permissionService.deleteAcl(toObjectIdentity(TaskCategory.class, id));
    }

}
