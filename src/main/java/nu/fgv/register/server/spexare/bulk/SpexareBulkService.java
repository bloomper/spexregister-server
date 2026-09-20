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

package nu.fgv.register.server.spexare.bulk;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.SpexareService;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityRepository;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivityRepository;
import nu.fgv.register.server.spexare.consent.Consent;
import nu.fgv.register.server.spexare.consent.ConsentRepository;
import nu.fgv.register.server.spexare.toggle.Toggle;
import nu.fgv.register.server.spexare.toggle.ToggleRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static nu.fgv.register.server.settings.TypeMapper.TYPE_MAPPER;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexareBulkService {

    static final int MAX_TARGET_SIZE = 500;

    private static final Sort TARGET_SORT = Sort.by("lastName", "firstName");

    private final SpexareService spexareService;
    private final SpexareRepository spexareRepository;
    private final TagRepository tagRepository;
    private final SpexRepository spexRepository;
    private final TaskRepository taskRepository;
    private final TypeRepository typeRepository;
    private final ConsentRepository consentRepository;
    private final ToggleRepository toggleRepository;
    private final ActivityRepository activityRepository;
    private final SpexActivityRepository spexActivityRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final PermissionService permissionService;

    private static BulkEntryDto entry(final Spexare spexare, final BulkOutcome outcome, final String detail) {
        return BulkEntryDto.builder()
                .id(spexare.getId())
                .label(labelOf(spexare))
                .outcome(outcome)
                .detail(detail)
                .build();
    }

    private static <T> String labels(final Collection<T> items, final Function<T, String> label) {
        return items.stream().map(label).collect(Collectors.joining(", "));
    }

    private static String labelOf(final Spexare spexare) {
        return "%s %s".formatted(spexare.getFirstName(), spexare.getLastName());
    }

    private static String labelOf(final Spex spex) {
        return "%s %s".formatted(spex.getYear(), spex.getDetails().getTitle());
    }

    @RequiresAdminOrEditorOrUser
    public BulkResultDto preview(final SpexareBulkInputDto input) {
        return run(input, true);
    }

    @RequiresAdminOrEditorOrUser
    public BulkResultDto apply(final SpexareBulkInputDto input) {
        return run(input, false);
    }

    private BulkResultDto run(final SpexareBulkInputDto input, final boolean dryRun) {
        final Handler handler = handlerFor(input);
        final List<Spexare> targets = resolveTargets(input.target());
        final List<BulkEntryDto> entries = new ArrayList<>(targets.size());

        for (final Spexare spexare : targets) {
            entries.add(permissionService.hasWritePermission(spexare) ?
                    handler.handle(spexare, dryRun) :
                    entry(spexare, BulkOutcome.NOT_PERMITTED, null));
        }

        return BulkResultDto.of(input.operation(), entries);
    }

    private List<Spexare> resolveTargets(final BulkTargetDto target) {
        final List<Long> ids = Optional.ofNullable(target.ids()).orElseGet(List::of);
        final String filter = Optional.ofNullable(target.filter()).orElse("");

        if (ids.isEmpty() && !hasText(filter)) {
            throw new BadRequestException("A bulk operation needs either ids or a filter");
        }
        if (!ids.isEmpty() && hasText(filter)) {
            throw new BadRequestException("A bulk operation takes either ids or a filter, not both");
        }

        final long total = ids.isEmpty() ? spexareService.find(filter, PageRequest.of(0, 1)).getTotalElements() : ids.size();

        if (total > MAX_TARGET_SIZE) {
            throw new BadRequestException("A bulk operation is limited to %d records, got %d".formatted(MAX_TARGET_SIZE, total));
        }

        return StreamSupport
                .stream(spexareService.streamByIds(ids, filter, TARGET_SORT).spliterator(), false)
                .toList();
    }

    // Resolving the payload up front means a bad id fails the whole batch before anything is written.
    private Handler handlerFor(final SpexareBulkInputDto input) {
        return switch (input.operation()) {
            case TAG_ADD -> {
                final List<Tag> tags = resolve(input.tags(), tagRepository::findById, Tag.class, "tags");
                yield (spexare, dryRun) -> handleTags(spexare, tags, true, dryRun);
            }
            case TAG_REMOVE -> {
                final List<Tag> tags = resolve(input.tags(), tagRepository::findById, Tag.class, "tags");
                yield (spexare, dryRun) -> handleTags(spexare, tags, false, dryRun);
            }
            case SPEX_ADD -> {
                final List<Spex> spex = resolve(input.spex(), spexRepository::findById, Spex.class, "spex");
                yield (spexare, dryRun) -> handleSpexAdd(spexare, spex, dryRun);
            }
            case SPEX_REMOVE -> {
                final List<Spex> spex = resolve(input.spex(), spexRepository::findById, Spex.class, "spex");
                yield (spexare, dryRun) -> handleSpexRemove(spexare, spex, dryRun);
            }
            case TASK_ADD -> {
                final Spex spex = requireSpex(input);
                final List<Task> tasks = resolve(input.tasks(), taskRepository::findById, Task.class, "tasks");
                yield (spexare, dryRun) -> handleTaskAdd(spexare, spex, tasks, dryRun);
            }
            case TASK_REMOVE -> {
                final Spex spex = requireSpex(input);
                final List<Task> tasks = resolve(input.tasks(), taskRepository::findById, Task.class, "tasks");
                yield (spexare, dryRun) -> handleTaskRemove(spexare, spex, tasks, dryRun);
            }
            case CONSENT_SET -> {
                final List<TypedValue> values = resolveTypedValues(input.values(), TypeType.CONSENT);
                yield (spexare, dryRun) -> handleConsentSet(spexare, values, dryRun);
            }
            case TOGGLE_SET -> {
                final List<TypedValue> values = resolveTypedValues(input.values(), TypeType.TOGGLE);
                yield (spexare, dryRun) -> handleToggleSet(spexare, values, dryRun);
            }
            case FIELDS_SET -> {
                final SpexareBulkFieldsDto fields = input.fields();

                if (fields == null || fields.isEmpty()) {
                    throw new BadRequestException("FIELDS_SET needs at least one field");
                }
                yield (spexare, dryRun) -> handleFieldsSet(spexare, fields, dryRun);
            }
        };
    }

    private BulkEntryDto handleTags(final Spexare spexare, final List<Tag> tags, final boolean add, final boolean dryRun) {
        final Set<Tag> current = spexare.getTags() == null ? new HashSet<>() : spexare.getTags();
        final List<Tag> affected = tags.stream()
                .filter(tag -> current.contains(tag) != add)
                .toList();

        if (affected.isEmpty()) {
            return entry(spexare, BulkOutcome.UNCHANGED, null);
        }

        if (!dryRun) {
            spexare.setTags(current);
            if (add) {
                current.addAll(affected);
            } else {
                current.removeAll(affected);
            }
            touch(spexare);
        }

        return entry(spexare, BulkOutcome.APPLIED, labels(affected, Tag::getName));
    }

    private BulkEntryDto handleSpexAdd(final Spexare spexare, final List<Spex> spex, final boolean dryRun) {
        final List<Spex> missing = spex.stream()
                .filter(s -> findActivity(spexare, s).isEmpty())
                .toList();

        if (missing.isEmpty()) {
            return entry(spexare, BulkOutcome.UNCHANGED, null);
        }

        if (!dryRun) {
            missing.forEach(s -> createActivityFor(spexare, s));
        }

        return entry(spexare, BulkOutcome.APPLIED, labels(missing, SpexareBulkService::labelOf));
    }

    private BulkEntryDto handleSpexRemove(final Spexare spexare, final List<Spex> spex, final boolean dryRun) {
        final List<Spex> present = spex.stream()
                .filter(s -> findActivity(spexare, s).isPresent())
                .toList();

        if (present.isEmpty()) {
            return entry(spexare, BulkOutcome.UNCHANGED, null);
        }

        if (!dryRun) {
            present.forEach(s -> findActivity(spexare, s).ifPresent(activity -> {
                if (spexare.getActivities() != null) {
                    spexare.getActivities().remove(activity);
                }
                activityRepository.delete(activity);
            }));
        }

        return entry(spexare, BulkOutcome.APPLIED, labels(present, SpexareBulkService::labelOf));
    }

    private BulkEntryDto handleTaskAdd(final Spexare spexare, final Spex spex, final List<Task> tasks, final boolean dryRun) {
        final Optional<Activity> existing = findActivity(spexare, spex);
        final List<Task> missing = existing
                .map(activity -> tasks.stream().filter(task -> findTaskActivity(activity, task).isEmpty()).toList())
                .orElse(tasks);

        if (missing.isEmpty()) {
            return entry(spexare, BulkOutcome.UNCHANGED, null);
        }

        if (!dryRun) {
            final Activity activity = existing.orElseGet(() -> createActivityFor(spexare, spex));

            missing.forEach(task -> {
                final TaskActivity taskActivity = new TaskActivity();

                taskActivity.setActivity(activity);
                taskActivity.setTask(task);
                taskActivityRepository.save(taskActivity);
            });
        }

        return entry(spexare, BulkOutcome.APPLIED, labels(missing, Task::getName));
    }

    private BulkEntryDto handleTaskRemove(final Spexare spexare, final Spex spex, final List<Task> tasks, final boolean dryRun) {
        final Optional<Activity> existing = findActivity(spexare, spex);

        if (existing.isEmpty()) {
            return entry(spexare, BulkOutcome.UNCHANGED, null);
        }

        final Activity activity = existing.get();
        final List<Task> present = tasks.stream()
                .filter(task -> findTaskActivity(activity, task).isPresent())
                .toList();

        if (present.isEmpty()) {
            return entry(spexare, BulkOutcome.UNCHANGED, null);
        }

        if (!dryRun) {
            present.forEach(task -> findTaskActivity(activity, task).ifPresent(taskActivity -> {
                if (activity.getTaskActivities() != null) {
                    activity.getTaskActivities().remove(taskActivity);
                }
                taskActivityRepository.delete(taskActivity);
            }));
        }

        return entry(spexare, BulkOutcome.APPLIED, labels(present, Task::getName));
    }

    private BulkEntryDto handleConsentSet(final Spexare spexare, final List<TypedValue> values, final boolean dryRun) {
        final List<TypedValue> affected = values.stream()
                .filter(v -> findConsent(spexare, v.type())
                        .map(consent -> !Objects.equals(consent.getValue(), v.value()))
                        .orElse(true))
                .toList();

        if (affected.isEmpty()) {
            return entry(spexare, BulkOutcome.UNCHANGED, null);
        }

        if (!dryRun) {
            affected.forEach(v -> findConsent(spexare, v.type())
                    .ifPresentOrElse(
                            consent -> {
                                consent.setValue(v.value());
                                consentRepository.save(consent);
                            },
                            () -> {
                                final Consent consent = new Consent();

                                consent.setSpexare(spexare);
                                consent.setType(v.type());
                                consent.setValue(v.value());
                                consentRepository.save(consent);
                            }));
        }

        return entry(spexare, BulkOutcome.APPLIED, labels(affected, TypedValue::label));
    }

    private BulkEntryDto handleToggleSet(final Spexare spexare, final List<TypedValue> values, final boolean dryRun) {
        final List<TypedValue> affected = values.stream()
                .filter(v -> findToggle(spexare, v.type())
                        .map(toggle -> !Objects.equals(toggle.getValue(), v.value()))
                        .orElse(true))
                .toList();

        if (affected.isEmpty()) {
            return entry(spexare, BulkOutcome.UNCHANGED, null);
        }

        if (!dryRun) {
            affected.forEach(v -> findToggle(spexare, v.type())
                    .ifPresentOrElse(
                            toggle -> {
                                toggle.setValue(v.value());
                                toggleRepository.save(toggle);
                            },
                            () -> {
                                final Toggle toggle = new Toggle();

                                toggle.setSpexare(spexare);
                                toggle.setType(v.type());
                                toggle.setValue(v.value());
                                toggleRepository.save(toggle);
                            }));
        }

        return entry(spexare, BulkOutcome.APPLIED, labels(affected, TypedValue::label));
    }

    private BulkEntryDto handleFieldsSet(final Spexare spexare, final SpexareBulkFieldsDto fields, final boolean dryRun) {
        final List<String> affected = new ArrayList<>(2);

        if (fields.published() != null && !Objects.equals(spexare.getPublished(), fields.published())) {
            affected.add("published");
        }
        if (fields.deceased() != null && !Objects.equals(spexare.getDeceased(), fields.deceased())) {
            affected.add("deceased");
        }

        if (affected.isEmpty()) {
            return entry(spexare, BulkOutcome.UNCHANGED, null);
        }

        if (!dryRun) {
            if (fields.published() != null) {
                spexare.setPublished(fields.published());
            }
            if (fields.deceased() != null) {
                spexare.setDeceased(fields.deceased());
            }
            touch(spexare);

            if (fields.published() != null) {
                final ObjectIdentity oid = toObjectIdentity(Spexare.class, spexare.getId());

                if (fields.published()) {
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_USER_SID);
                } else {
                    permissionService.revokePermission(oid, BasePermission.READ, ROLE_USER_SID);
                }
            }
        }

        return entry(spexare, BulkOutcome.APPLIED, String.join(", ", affected));
    }

    private Activity createActivityFor(final Spexare spexare, final Spex spex) {
        final Activity activity = new Activity();

        activity.setSpexare(spexare);
        activityRepository.save(activity);

        final SpexActivity spexActivity = new SpexActivity();

        spexActivity.setActivity(activity);
        spexActivity.setSpex(spex);
        spexActivityRepository.save(spexActivity);
        activity.setSpexActivity(spexActivity);

        if (spexare.getActivities() == null) {
            spexare.setActivities(new HashSet<>());
        }
        spexare.getActivities().add(activity);

        return activity;
    }

    private Optional<Activity> findActivity(final Spexare spexare, final Spex spex) {
        return Optional.ofNullable(spexare.getActivities()).orElseGet(Set::of).stream()
                .filter(activity -> activity.getSpexActivity() != null)
                .filter(activity -> spex.equals(activity.getSpexActivity().getSpex()))
                .findFirst();
    }

    private Optional<TaskActivity> findTaskActivity(final Activity activity, final Task task) {
        return Optional.ofNullable(activity.getTaskActivities()).orElseGet(Set::of).stream()
                .filter(taskActivity -> task.equals(taskActivity.getTask()))
                .findFirst();
    }

    private Optional<Consent> findConsent(final Spexare spexare, final Type type) {
        return Optional.ofNullable(spexare.getConsents()).orElseGet(Set::of).stream()
                .filter(consent -> type.equals(consent.getType()))
                .findFirst();
    }

    private Optional<Toggle> findToggle(final Spexare spexare, final Type type) {
        return Optional.ofNullable(spexare.getToggles()).orElseGet(Set::of).stream()
                .filter(toggle -> type.equals(toggle.getType()))
                .findFirst();
    }

    private void touch(final Spexare spexare) {
        spexare.setLastModifiedAt(Instant.now());
        spexareRepository.save(spexare);
    }

    private Spex requireSpex(final SpexareBulkInputDto input) {
        if (input.spexId() == null) {
            throw new BadRequestException("%s needs a spexId".formatted(input.operation()));
        }
        return spexRepository
                .findById(input.spexId())
                .orElseThrow(() -> new ResourceNotFoundException(Spex.class, input.spexId()));
    }

    private <T> List<T> resolve(final List<Long> ids,
                                final Function<Long, Optional<T>> lookup,
                                final Class<T> type,
                                final String field) {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("A bulk operation needs at least one entry in '%s'".formatted(field));
        }
        return ids.stream()
                .distinct()
                .map(id -> lookup.apply(id).orElseThrow(() -> new ResourceNotFoundException(type, id)))
                .toList();
    }

    private List<TypedValue> resolveTypedValues(final List<TypeValueDto> values, final TypeType expected) {
        if (values == null || values.isEmpty()) {
            throw new BadRequestException("A bulk operation needs at least one entry in 'values'");
        }
        return values.stream()
                .map(value -> {
                    final Type type = typeRepository
                            .findById(value.typeId())
                            .orElseThrow(() -> new ResourceNotFoundException(Type.class, value.typeId()));

                    if (type.getType() != expected) {
                        throw new BadRequestException("Type '%s' is not a %s type".formatted(value.typeId(), expected));
                    }
                    return new TypedValue(type, value.value(), TYPE_MAPPER.toDto(type).getLabel());
                })
                .toList();
    }

    @FunctionalInterface
    private interface Handler {
        BulkEntryDto handle(Spexare spexare, boolean dryRun);
    }

    private record TypedValue(Type type, Boolean value, String label) {
    }
}
