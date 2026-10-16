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

package nu.fgv.register.server.audit;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import nu.fgv.register.server.news.News;
import nu.fgv.register.server.news.NewsRepository;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spex.SpexDetailsRepository;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.ActivityRepository;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityRepository;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivityRepository;
import nu.fgv.register.server.spexare.activity.task.actor.Actor;
import nu.fgv.register.server.spexare.activity.task.actor.ActorRepository;
import nu.fgv.register.server.spexare.address.Address;
import nu.fgv.register.server.spexare.address.AddressRepository;
import nu.fgv.register.server.spexare.consent.Consent;
import nu.fgv.register.server.spexare.consent.ConsentRepository;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.membership.MembershipRepository;
import nu.fgv.register.server.spexare.toggle.Toggle;
import nu.fgv.register.server.spexare.toggle.ToggleRepository;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.user.User;
import nu.fgv.register.server.user.UserRepository;
import nu.fgv.register.server.user.state.State;
import nu.fgv.register.server.user.state.StateRepository;
import nu.fgv.register.server.util.error.BadRequestException;
import org.hibernate.Hibernate;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Holds one {@link AuditedEntityDescriptor} per audited entity, keyed by {@link AuditedType}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class AuditRegistry {

    private final NewsRepository newsRepository;
    private final SpexRepository spexRepository;
    private final SpexDetailsRepository spexDetailsRepository;
    private final SpexCategoryRepository spexCategoryRepository;
    private final SpexareRepository spexareRepository;
    private final ActivityRepository activityRepository;
    private final SpexActivityRepository spexActivityRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final ActorRepository actorRepository;
    private final AddressRepository addressRepository;
    private final ConsentRepository consentRepository;
    private final MembershipRepository membershipRepository;
    private final ToggleRepository toggleRepository;
    private final TagRepository tagRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final UserRepository userRepository;
    private final StateRepository stateRepository;
    private final TypeRepository typeRepository;

    private final Map<AuditedType, AuditedEntityDescriptor> descriptors = new EnumMap<>(AuditedType.class);
    private final Map<String, AuditedType> typesByEntityName = new java.util.HashMap<>();

    private static List<AuditedChildDescriptor> spexareChildren() {
        return List.of(
                collectionChild(AuditedType.ADDRESS, Address.class, "spexare", Spexare::getAddresses, Spexare::setAddresses, Address::setSpexare, List.of()),
                collectionChild(AuditedType.CONSENT, Consent.class, "spexare", Spexare::getConsents, Spexare::setConsents, Consent::setSpexare, List.of()),
                collectionChild(AuditedType.MEMBERSHIP, Membership.class, "spexare", Spexare::getMemberships, Spexare::setMemberships, Membership::setSpexare, List.of()),
                collectionChild(AuditedType.TOGGLE, Toggle.class, "spexare", Spexare::getToggles, Spexare::setToggles, Toggle::setSpexare, List.of()),
                collectionChild(AuditedType.ACTIVITY, Activity.class, "spexare", Spexare::getActivities, Spexare::setActivities, Activity::setSpexare, activityChildren())
        );
    }

    private static List<AuditedChildDescriptor> activityChildren() {
        return List.of(
                AuditedChildDescriptor.of(AuditedType.SPEX_ACTIVITY, SpexActivity.class, "activity",
                        (final Activity a) -> a.getSpexActivity() == null ? List.of() : List.of(a.getSpexActivity()),
                        (final SpexActivity c, final Activity p) -> {
                            c.setActivity(p);
                            p.setSpexActivity(c);
                        },
                        (final SpexActivity c, final Activity p) -> p.setSpexActivity(null),
                        List.of()),
                collectionChild(AuditedType.TASK_ACTIVITY, TaskActivity.class, "activity", Activity::getTaskActivities, Activity::setTaskActivities, TaskActivity::setActivity, taskActivityChildren())
        );
    }

    private static List<AuditedChildDescriptor> taskActivityChildren() {
        return List.of(
                collectionChild(AuditedType.ACTOR, Actor.class, "taskActivity", TaskActivity::getActors, TaskActivity::setActors, Actor::setTaskActivity, List.of())
        );
    }

    private static <P, C> AuditedChildDescriptor collectionChild(
            final AuditedType type,
            final Class<C> entityClass,
            final String parentProperty,
            final Function<P, @Nullable Set<C>> getChildren,
            final BiConsumer<P, Set<C>> setChildren,
            final BiConsumer<C, P> setParent,
            final List<AuditedChildDescriptor> children
    ) {
        return AuditedChildDescriptor.of(type, entityClass, parentProperty,
                (final P p) -> {
                    final Set<C> current = getChildren.apply(p);

                    return current == null ? Set.of() : current;
                },
                (final C c, final P p) -> {
                    setParent.accept(c, p);

                    Set<C> current = getChildren.apply(p);

                    if (current == null) {
                        current = new HashSet<>();
                        setChildren.accept(p, current);
                    }
                    current.add(c);
                },
                (final C c, final P p) -> {
                    final Set<C> current = getChildren.apply(p);

                    if (current != null) {
                        current.remove(c);
                    }
                },
                children);
    }

    private static <E> Function<E, @Nullable Object> self() {
        return e -> e;
    }

    private static String localized(final @Nullable Map<String, String> labels, final String fallback) {
        if (labels == null) {
            return fallback;
        }

        return labels.getOrDefault(LocaleContextHolder.getLocale().getLanguage(), fallback);
    }

    private static String fullName(final @Nullable String firstName, final @Nullable String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    @PostConstruct
    void initialize() {
        register(AuditedEntityDescriptor.of(AuditedType.NEWS, News.class, Long::valueOf, newsRepository::findById, self(), List.of()).withLabel(News::getSubject));
        register(AuditedEntityDescriptor.of(AuditedType.SPEX, Spex.class, Long::valueOf, spexRepository::findById, self(), List.of()).withLabel(Spex::getYear).withReferences(new AuditedReferenceDescriptor(AuditedType.SPEX_DETAILS, SpexDetails.class, "details")));
        register(AuditedEntityDescriptor.of(AuditedType.SPEX_DETAILS, SpexDetails.class, Long::valueOf, spexDetailsRepository::findById, (SpexDetails d) -> spexRepository.findByDetailsAndParentIsNull(d).orElse(null), List.of()).withLabel(SpexDetails::getTitle));
        register(AuditedEntityDescriptor.of(AuditedType.SPEX_CATEGORY, SpexCategory.class, Long::valueOf, spexCategoryRepository::findById, self(), List.of()).withLabel(SpexCategory::getName));
        register(AuditedEntityDescriptor.of(AuditedType.TAG, Tag.class, Long::valueOf, tagRepository::findById, self(), List.of()).withLabel(Tag::getName));
        register(AuditedEntityDescriptor.of(AuditedType.TASK, Task.class, Long::valueOf, taskRepository::findById, self(), List.of()).withLabel(Task::getName));
        register(AuditedEntityDescriptor.of(AuditedType.TASK_CATEGORY, TaskCategory.class, Long::valueOf, taskCategoryRepository::findById, self(), List.of()).withLabel(TaskCategory::getName));
        register(AuditedEntityDescriptor.of(AuditedType.USER, User.class, Long::valueOf, userRepository::findById, self(), List.of()).withLabel((User u) -> u.getSpexare() == null ? u.getExternalId() : fullName(u.getSpexare().getFirstName(), u.getSpexare().getLastName())));
        register(AuditedEntityDescriptor.of(AuditedType.STATE, State.class, Function.identity(), stateRepository::findById, null, List.of()).withLabel((State st) -> localized(st.getLabels(), st.getId())));
        register(AuditedEntityDescriptor.of(AuditedType.TYPE, Type.class, Function.identity(), typeRepository::findById, null, List.of()).withLabel((Type ty) -> localized(ty.getLabels(), ty.getId())));

        register(AuditedEntityDescriptor.of(AuditedType.SPEXARE, Spexare.class, Long::valueOf, spexareRepository::findById, self(), spexareChildren()).withLabel((Spexare sp) -> fullName(sp.getFirstName(), sp.getLastName())));
        register(AuditedEntityDescriptor.of(AuditedType.ADDRESS, Address.class, Long::valueOf, addressRepository::findById, Address::getSpexare, List.of()).withLabel(Address::getStreetAddress));
        register(AuditedEntityDescriptor.of(AuditedType.CONSENT, Consent.class, Long::valueOf, consentRepository::findById, Consent::getSpexare, List.of()).withLabel((Consent c) -> localized(c.getType().getLabels(), c.getType().getId())));
        register(AuditedEntityDescriptor.of(AuditedType.MEMBERSHIP, Membership.class, Long::valueOf, membershipRepository::findById, Membership::getSpexare, List.of()).withLabel(Membership::getYear));
        register(AuditedEntityDescriptor.of(AuditedType.TOGGLE, Toggle.class, Long::valueOf, toggleRepository::findById, Toggle::getSpexare, List.of()).withLabel((Toggle t) -> localized(t.getType().getLabels(), t.getType().getId())));
        register(AuditedEntityDescriptor.of(AuditedType.ACTIVITY, Activity.class, Long::valueOf, activityRepository::findById, Activity::getSpexare, activityChildren()).withLabel((Activity a) -> fullName(a.getSpexare().getFirstName(), a.getSpexare().getLastName())));
        register(AuditedEntityDescriptor.of(AuditedType.SPEX_ACTIVITY, SpexActivity.class, Long::valueOf, spexActivityRepository::findById,
                a -> a.getActivity().getSpexare(), List.of()).withLabel((SpexActivity sa) -> sa.getSpex().getYear()));
        register(AuditedEntityDescriptor.of(AuditedType.TASK_ACTIVITY, TaskActivity.class, Long::valueOf, taskActivityRepository::findById,
                a -> a.getActivity().getSpexare(), taskActivityChildren()).withLabel((TaskActivity ta) -> ta.getTask().getName()));
        register(AuditedEntityDescriptor.of(AuditedType.ACTOR, Actor.class, Long::valueOf, actorRepository::findById,
                a -> a.getTaskActivity().getActivity().getSpexare(), List.of()).withLabel(Actor::getRole));
    }

    private void register(final AuditedEntityDescriptor descriptor) {
        descriptors.put(descriptor.type(), descriptor);
        typesByEntityName.put(descriptor.entityClass().getName(), descriptor.type());
    }

    public AuditedEntityDescriptor require(final AuditedType type) {
        final AuditedEntityDescriptor descriptor = descriptors.get(type);

        if (descriptor == null) {
            throw new BadRequestException("Unsupported audited type %s".formatted(type));
        }

        return descriptor;
    }

    public @Nullable String labelFor(final Object entity) {
        final AuditedEntityDescriptor descriptor = descriptors.get(typesByEntityName.get(Hibernate.getClass(entity).getName()));

        if (descriptor == null || descriptor.label() == null) {
            return null;
        }

        try {
            return descriptor.label().apply(Hibernate.unproxy(entity));
        } catch (final RuntimeException e) {
            return null;
        }
    }

    public @Nullable AuditedType byEntityName(final String entityName) {
        return typesByEntityName.get(entityName);
    }

    public Optional<AuditedEntityDescriptor> find(final AuditedType type) {
        return Optional.ofNullable(descriptors.get(type));
    }
}