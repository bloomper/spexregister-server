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

import jakarta.annotation.PostConstruct;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @PostConstruct
    void initialize() {
        register(AuditedEntityDescriptor.of(AuditedType.NEWS, News.class, Long::valueOf, newsRepository::findById, self(), List.of()).withLabel((News n) -> n.getSubject()));
        register(AuditedEntityDescriptor.of(AuditedType.SPEX, Spex.class, Long::valueOf, spexRepository::findById, self(), List.of()).withLabel((Spex x) -> x.getYear()).withReferences(new AuditedReferenceDescriptor(AuditedType.SPEX_DETAILS, SpexDetails.class, "details")));
        register(AuditedEntityDescriptor.of(AuditedType.SPEX_DETAILS, SpexDetails.class, Long::valueOf, spexDetailsRepository::findById, self(), List.of()).withLabel((SpexDetails d) -> d.getTitle()));
        register(AuditedEntityDescriptor.of(AuditedType.SPEX_CATEGORY, SpexCategory.class, Long::valueOf, spexCategoryRepository::findById, self(), List.of()).withLabel((SpexCategory c) -> c.getName()));
        register(AuditedEntityDescriptor.of(AuditedType.TAG, Tag.class, Long::valueOf, tagRepository::findById, self(), List.of()).withLabel((Tag t) -> t.getName()));
        register(AuditedEntityDescriptor.of(AuditedType.TASK, Task.class, Long::valueOf, taskRepository::findById, self(), List.of()).withLabel((Task t) -> t.getName()));
        register(AuditedEntityDescriptor.of(AuditedType.TASK_CATEGORY, TaskCategory.class, Long::valueOf, taskCategoryRepository::findById, self(), List.of()).withLabel((TaskCategory c) -> c.getName()));
        register(AuditedEntityDescriptor.of(AuditedType.USER, User.class, Long::valueOf, userRepository::findById, self(), List.of()).withLabel((User u) -> u.getSpexare() == null ? u.getExternalId() : fullName(u.getSpexare().getFirstName(), u.getSpexare().getLastName())));
        register(AuditedEntityDescriptor.of(AuditedType.STATE, State.class, Function.identity(), stateRepository::findById, null, List.of()).withLabel((State st) -> localized(st.getLabels(), st.getId())));
        register(AuditedEntityDescriptor.of(AuditedType.TYPE, Type.class, Function.identity(), typeRepository::findById, null, List.of()).withLabel((Type ty) -> localized(ty.getLabels(), ty.getId())));

        register(AuditedEntityDescriptor.of(AuditedType.SPEXARE, Spexare.class, Long::valueOf, spexareRepository::findById, self(), spexareChildren()).withLabel((Spexare sp) -> fullName(sp.getFirstName(), sp.getLastName())));
        register(AuditedEntityDescriptor.of(AuditedType.ADDRESS, Address.class, Long::valueOf, addressRepository::findById, Address::getSpexare, List.of()).withLabel((Address a) -> a.getStreetAddress()));
        register(AuditedEntityDescriptor.of(AuditedType.CONSENT, Consent.class, Long::valueOf, consentRepository::findById, Consent::getSpexare, List.of()).withLabel((Consent c) -> c.getType() == null ? null : localized(c.getType().getLabels(), c.getType().getId())));
        register(AuditedEntityDescriptor.of(AuditedType.MEMBERSHIP, Membership.class, Long::valueOf, membershipRepository::findById, Membership::getSpexare, List.of()).withLabel((Membership m) -> m.getYear()));
        register(AuditedEntityDescriptor.of(AuditedType.TOGGLE, Toggle.class, Long::valueOf, toggleRepository::findById, Toggle::getSpexare, List.of()).withLabel((Toggle t) -> t.getType() == null ? null : localized(t.getType().getLabels(), t.getType().getId())));
        register(AuditedEntityDescriptor.of(AuditedType.ACTIVITY, Activity.class, Long::valueOf, activityRepository::findById, Activity::getSpexare, activityChildren()).withLabel((Activity a) -> a.getSpexare() == null ? null : fullName(a.getSpexare().getFirstName(), a.getSpexare().getLastName())));
        register(AuditedEntityDescriptor.of(AuditedType.SPEX_ACTIVITY, SpexActivity.class, Long::valueOf, spexActivityRepository::findById,
                a -> a.getActivity() == null ? null : a.getActivity().getSpexare(), List.of()).withLabel((SpexActivity sa) -> sa.getSpex() == null ? null : sa.getSpex().getYear()));
        register(AuditedEntityDescriptor.of(AuditedType.TASK_ACTIVITY, TaskActivity.class, Long::valueOf, taskActivityRepository::findById,
                a -> a.getActivity() == null ? null : a.getActivity().getSpexare(), taskActivityChildren()).withLabel((TaskActivity ta) -> ta.getTask() == null ? null : ta.getTask().getName()));
        register(AuditedEntityDescriptor.of(AuditedType.ACTOR, Actor.class, Long::valueOf, actorRepository::findById,
                a -> a.getTaskActivity() == null || a.getTaskActivity().getActivity() == null ? null : a.getTaskActivity().getActivity().getSpexare(), List.of()).withLabel((Actor a) -> a.getRole()));
    }

    private static List<AuditedChildDescriptor> spexareChildren() {
        return List.of(
                collectionChild(AuditedType.ADDRESS, Address.class, "spexare", Spexare::getAddresses, Address::setSpexare),
                collectionChild(AuditedType.CONSENT, Consent.class, "spexare", Spexare::getConsents, Consent::setSpexare),
                collectionChild(AuditedType.MEMBERSHIP, Membership.class, "spexare", Spexare::getMemberships, Membership::setSpexare),
                collectionChild(AuditedType.TOGGLE, Toggle.class, "spexare", Spexare::getToggles, Toggle::setSpexare),
                AuditedChildDescriptor.of(AuditedType.ACTIVITY, Activity.class, "spexare",
                        (Spexare s) -> (Collection<Activity>) s.getActivities(),
                        (Activity c, Spexare p) -> {
                            c.setSpexare(p);
                            p.getActivities().add(c);
                        },
                        (Activity c, Spexare p) -> p.getActivities().remove(c),
                        activityChildren())
        );
    }

    private static List<AuditedChildDescriptor> activityChildren() {
        return List.of(
                AuditedChildDescriptor.of(AuditedType.SPEX_ACTIVITY, SpexActivity.class, "activity",
                        (Activity a) -> a.getSpexActivity() == null ? List.of() : List.of(a.getSpexActivity()),
                        (SpexActivity c, Activity p) -> {
                            c.setActivity(p);
                            p.setSpexActivity(c);
                        },
                        (SpexActivity c, Activity p) -> p.setSpexActivity(null),
                        List.of()),
                AuditedChildDescriptor.of(AuditedType.TASK_ACTIVITY, TaskActivity.class, "activity",
                        (Activity a) -> (Collection<TaskActivity>) a.getTaskActivities(),
                        (TaskActivity c, Activity p) -> {
                            c.setActivity(p);
                            p.getTaskActivities().add(c);
                        },
                        (TaskActivity c, Activity p) -> p.getTaskActivities().remove(c),
                        taskActivityChildren())
        );
    }

    private static List<AuditedChildDescriptor> taskActivityChildren() {
        return List.of(
                AuditedChildDescriptor.of(AuditedType.ACTOR, Actor.class, "taskActivity",
                        (TaskActivity t) -> (Collection<Actor>) t.getActors(),
                        (Actor c, TaskActivity p) -> {
                            c.setTaskActivity(p);
                            p.getActors().add(c);
                        },
                        (Actor c, TaskActivity p) -> p.getActors().remove(c),
                        List.of())
        );
    }

    private static <P, C> AuditedChildDescriptor collectionChild(
            final AuditedType type,
            final Class<C> entityClass,
            final String parentProperty,
            final Function<P, Collection<C>> childrenOf,
            final java.util.function.BiConsumer<C, P> setParent
    ) {
        return AuditedChildDescriptor.of(type, entityClass, parentProperty, childrenOf,
                (C c, P p) -> {
                    setParent.accept(c, p);
                    childrenOf.apply(p).add(c);
                },
                (C c, P p) -> childrenOf.apply(p).remove(c),
                List.of());
    }

    private static <E> Function<E, Object> self() {
        return e -> e;
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

    private static @Nullable String localized(final @Nullable Map<String, String> labels, final String fallback) {
        if (labels == null) {
            return fallback;
        }

        return labels.getOrDefault(LocaleContextHolder.getLocale().getLanguage(), fallback);
    }

    private static String fullName(final @Nullable String firstName, final @Nullable String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    public @Nullable AuditedType byEntityName(final String entityName) {
        return typesByEntityName.get(entityName);
    }

    public Optional<AuditedEntityDescriptor> find(final AuditedType type) {
        return Optional.ofNullable(descriptors.get(type));
    }
}