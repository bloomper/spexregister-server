package nu.fgv.register.server.task;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;
import static nu.fgv.register.server.task.TaskSpecification.hasIds;
import static nu.fgv.register.server.task.category.TaskCategoryMapper.TASK_CATEGORY_MAPPER;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaskService {

    private final TaskRepository repository;
    private final TaskCategoryRepository categoryRepository;
    private final PermissionService permissionService;

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<TaskDto> findAll(final Sort sort) {
        return repository
                .findAll(sort, BasePermission.READ)
                .stream()
                .map(TASK_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Page<TaskDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Task>builder().build(FilterParser.parse(filter), TaskSpecification::new), pageable, BasePermission.READ)
                        .map(TASK_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(TASK_MAPPER::toDto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Optional<TaskDto> findById(final Long id) {
        return repository
                .findById0(id)
                .map(TASK_MAPPER::toDto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public List<TaskDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort, BasePermission.READ)
                .stream().map(TASK_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public TaskDto create(final TaskCreateDto dto) {
        return Optional.of(TASK_MAPPER.toModel(dto))
                .map(model -> {
                    final Task task = repository.save(model);
                    final ObjectIdentity oid = toObjectIdentity(Task.class, task.getId());

                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.DELETE, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_USER_SID);

                    return TASK_MAPPER.toDto(task);
                })
                .orElse(null);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public Optional<TaskDto> update(final TaskUpdateDto dto) {
        return partialUpdate(dto);
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR')")
    public Optional<TaskDto> partialUpdate(final TaskUpdateDto dto) {
        return repository
                .findById0(dto.getId())
                .map(task -> {
                    TASK_MAPPER.toPartialModel(dto, task);
                    return task;
                })
                .map(repository::save)
                .map(TASK_MAPPER::toDto);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public void deleteById(final Long id) {
        repository.deleteById(id);
        permissionService.deleteAcl(toObjectIdentity(Task.class, id));
    }

    @PreAuthorize("hasAnyRole('spexregister_ADMIN', 'spexregister_EDITOR', 'spexregister_USER')")
    public Optional<TaskCategoryDto> findCategoryByTask(final Long taskId) {
        if (doesTaskExist(taskId)) {
            return repository
                    .findById0(taskId)
                    .map(Task::getCategory)
                    .map(TASK_CATEGORY_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Task %s does not exist", taskId));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean addCategory(final Long taskId, final Long id) {
        if (doTaskAndCategoryExist(taskId, id)) {
            return repository
                    .findById0(taskId)
                    .map(task -> categoryRepository
                            .findById(id)
                            .map(category -> {
                                task.setCategory(category);
                                repository.save(task);
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Task %s and/or category %s do not exist", taskId, id));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean removeCategory(final Long taskId) {
        if (doesTaskExist(taskId)) {
            return repository
                    .findById0(taskId)
                    .filter(task -> task.getCategory() != null)
                    .map(task -> {
                        task.setCategory(null);
                        repository.save(task);
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Task %s does not exist", taskId));
        }
    }

    private boolean doesTaskExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doTaskAndCategoryExist(final Long taskId, final Long categoryId) {
        return doesTaskExist(taskId) && categoryRepository.existsById(categoryId);
    }

}
