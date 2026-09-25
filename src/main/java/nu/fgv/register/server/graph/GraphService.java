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

package nu.fgv.register.server.graph;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.image.Image;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexApi;
import nu.fgv.register.server.spex.SpexRepository;
import nu.fgv.register.server.spex.category.SpexCategory;
import nu.fgv.register.server.spex.category.SpexCategoryApi;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.spexare.Spexare;
import nu.fgv.register.server.spexare.SpexareApi;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.actor.Actor;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.tag.TagRepository;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.TaskRepository;
import nu.fgv.register.server.task.category.TaskCategory;
import nu.fgv.register.server.task.category.TaskCategoryRepository;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static nu.fgv.register.server.graph.GraphNodeDto.idOf;
import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserSubClaim;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class GraphService {

    private static final Sort BY_ID = Sort.by(Sort.Direction.ASC, "id");

    private final SpexareRepository spexareRepository;
    private final SpexRepository spexRepository;
    private final SpexCategoryRepository spexCategoryRepository;
    private final TaskRepository taskRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final TagRepository tagRepository;

    private static GraphqlUtil.ScrollRequest firstOnly(final int first) {
        return new GraphqlUtil.ScrollRequest(Optional.empty(), first, false);
    }

    private static <T> GraphGroupDto group(final GraphEdgeType type, final long totalCount, final Stream<T> items,
                                           final java.util.function.Function<T, GraphNodeDto> toNode,
                                           final java.util.function.Function<T, GraphEdgeDto> toEdge) {
        final List<T> materialised = items.toList();

        return new GraphGroupDto(type, totalCount,
                materialised.stream().map(toNode).toList(),
                materialised.stream().map(toEdge).toList());
    }

    private static String yearOf(final Activity activity) {
        return Optional.ofNullable(activity.getSpexActivity())
                .map(spexActivity -> spexActivity.getSpex().getYear())
                .orElse("");
    }

    private static String rolesOf(final TaskActivity taskActivity) {
        return taskActivity.getActors().stream()
                .map(Actor::getRole)
                .filter(role -> role != null && !role.isBlank())
                .reduce((a, b) -> "%s, %s".formatted(a, b))
                .orElse("");
    }

    private static GraphNodeDto nodeOf(final Spexare spexare) {
        return new GraphNodeDto(idOf(GraphNodeType.SPEXARE, spexare.getId()), GraphNodeType.SPEXARE,
                "%s %s".formatted(spexare.getFirstName(), spexare.getLastName()), spexare.getNickName(),
                imageUrl(spexare.getImage(), () -> methodOn(SpexareApi.class).downloadImage(spexare.getId())),
                false, spexare.getId());
    }

    private static GraphNodeDto nodeOf(final Spex spex) {
        return new GraphNodeDto(idOf(GraphNodeType.SPEX, spex.getId()), GraphNodeType.SPEX,
                spex.getDetails().getTitle(), spex.getYear(),
                imageUrl(spex.getDetails().getPoster(), () -> methodOn(SpexApi.class).downloadPoster(spex.getId())),
                spex.getParent() != null, spex.getId());
    }

    private static GraphNodeDto nodeOf(final SpexCategory category) {
        return new GraphNodeDto(idOf(GraphNodeType.SPEX_CATEGORY, category.getId()), GraphNodeType.SPEX_CATEGORY,
                category.getName(), null,
                imageUrl(category.getLogo(), () -> methodOn(SpexCategoryApi.class).downloadLogo(category.getId())),
                false, category.getId());
    }

    private static GraphNodeDto nodeOf(final Task task) {
        return new GraphNodeDto(idOf(GraphNodeType.TASK, task.getId()), GraphNodeType.TASK,
                task.getName(), null, null, false, task.getId());
    }

    private static GraphNodeDto nodeOf(final TaskCategory category) {
        return new GraphNodeDto(idOf(GraphNodeType.TASK_CATEGORY, category.getId()), GraphNodeType.TASK_CATEGORY,
                category.getName(), null, null, false, category.getId());
    }

    private static GraphNodeDto nodeOf(final Tag tag) {
        return new GraphNodeDto(idOf(GraphNodeType.TAG, tag.getId()), GraphNodeType.TAG,
                tag.getName(), null, null, false, tag.getId());
    }

    private static @Nullable String imageUrl(final @Nullable Image image, final Supplier<Object> endpoint) {
        return image == null ? null : linkTo(endpoint.get()).toUri().toString();
    }

    @RequiresAdminOrEditorOrUser
    public List<GraphNodeDto> search(final String q, final int first) {
        final String term = q.trim();

        if (term.isEmpty()) {
            return currentUserSpexare().or(this::randomSpexare).map(List::of).orElseGet(List::of);
        }

        final List<GraphNodeDto> nodes = new ArrayList<>();

        nodes.addAll(spexareRepository.findBy(GraphSpecification.spexareMatching(term), BasePermission.READ,
                query -> query.sortBy(BY_ID).limit(first).all()).stream().map(GraphService::nodeOf).toList());
        nodes.addAll(spexRepository.findBy(GraphSpecification.spexMatching(term), BasePermission.READ,
                query -> query.sortBy(BY_ID).limit(first).all()).stream().map(GraphService::nodeOf).toList());
        nodes.addAll(taskRepository.findBy(GraphSpecification.taskMatching(term), BasePermission.READ,
                query -> query.sortBy(BY_ID).limit(first).all()).stream().map(GraphService::nodeOf).toList());
        nodes.addAll(tagRepository.findBy(GraphSpecification.tagMatching(term), BasePermission.READ,
                query -> query.sortBy(BY_ID).limit(first).all()).stream().map(GraphService::nodeOf).toList());

        return nodes.stream()
                .sorted(Comparator.comparing(GraphNodeDto::label, String.CASE_INSENSITIVE_ORDER))
                .limit((long) first * 2)
                .toList();
    }

    @RequiresAdminOrEditorOrUser
    public Optional<GraphNeighbourhoodDto> findNeighbourhood(final GraphNodeType type, final Long id, final int first) {
        return switch (type) {
            case SPEXARE -> spexareRepository.findById0(id).map(spexare -> neighbourhoodOf(spexare, first));
            case SPEX -> spexRepository.findById0(id).map(spex -> neighbourhoodOf(spex, first));
            case SPEX_CATEGORY ->
                    spexCategoryRepository.findById0(id).map(category -> neighbourhoodOf(category, first));
            case TASK -> taskRepository.findById0(id).map(task -> neighbourhoodOf(task, first));
            case TASK_CATEGORY ->
                    taskCategoryRepository.findById0(id).map(category -> neighbourhoodOf(category, first));
            case TAG -> tagRepository.findById0(id).map(tag -> neighbourhoodOf(tag, first));
        };
    }

    @RequiresAdminOrEditorOrUser
    public CountedWindow<GraphNodeDto> findNeighbours(final GraphNodeType type, final Long id,
                                                      final GraphEdgeType edge, final GraphqlUtil.ScrollRequest scroll) {
        return switch (type) {
            case SPEX -> edge == GraphEdgeType.PARTICIPATION
                    ? spexareWindow(GraphSpecification.spexareInSpex(id), scroll)
                    : GraphqlUtil.emptyWindow();
            case TASK -> edge == GraphEdgeType.FUNCTION
                    ? spexareWindow(GraphSpecification.spexareWithTask(id), scroll)
                    : GraphqlUtil.emptyWindow();
            case TAG -> edge == GraphEdgeType.TAG
                    ? spexareWindow(GraphSpecification.spexareWithTag(id), scroll)
                    : GraphqlUtil.emptyWindow();
            case SPEX_CATEGORY -> edge == GraphEdgeType.CATEGORY
                    ? spexRepository.findBy(GraphSpecification.spexInCategory(id), BasePermission.READ, query -> {
                final long total = query.count();

                return CountedWindow.of(query.limit(scroll.limit()).sortBy(BY_ID)
                        .scroll(scroll.positionFor(total)).map(GraphService::nodeOf), total);
            })
                    : GraphqlUtil.emptyWindow();
            case TASK_CATEGORY -> edge == GraphEdgeType.CATEGORY
                    ? taskRepository.findBy(GraphSpecification.taskInCategory(id), BasePermission.READ, query -> {
                final long total = query.count();

                return CountedWindow.of(query.limit(scroll.limit()).sortBy(BY_ID)
                        .scroll(scroll.positionFor(total)).map(GraphService::nodeOf), total);
            })
                    : GraphqlUtil.emptyWindow();
            case SPEXARE -> GraphqlUtil.emptyWindow();
        };
    }

    private Optional<GraphNodeDto> currentUserSpexare() {
        final String externalId = getCurrentUserSubClaim();

        if (!hasText(externalId)) {
            return Optional.empty();
        }

        try {
            return spexareRepository.findByUserExternalId(externalId).map(GraphService::nodeOf);
        } catch (final AccessDeniedException e) {
            return Optional.empty();
        }
    }

    private Optional<GraphNodeDto> randomSpexare() {
        return spexareRepository.findBy(Specification.unrestricted(), BasePermission.READ, query -> {
            final long total = query.count();

            if (total == 0L) {
                return Optional.<Spexare>empty();
            }

            final long offset = ThreadLocalRandom.current().nextLong(total);

            return query.limit(1).sortBy(BY_ID)
                    .scroll(offset == 0L ? ScrollPosition.offset() : ScrollPosition.offset(offset - 1))
                    .getContent().stream().findFirst();
        }).map(GraphService::nodeOf);
    }

    private GraphNeighbourhoodDto neighbourhoodOf(final Spexare spexare, final int first) {
        final String origin = idOf(GraphNodeType.SPEXARE, spexare.getId());
        final List<GraphGroupDto> groups = new ArrayList<>();

        final List<Activity> activities = spexare.getActivities().stream()
                .filter(activity -> activity.getSpexActivity() != null)
                .sorted(Comparator.comparing(GraphService::yearOf).reversed())
                .toList();

        final List<Spex> spexList = activities.stream()
                .map(activity -> activity.getSpexActivity().getSpex())
                .distinct()
                .limit(first)
                .toList();

        groups.add(group(GraphEdgeType.PARTICIPATION, spexList.size(), spexList.stream(),
                GraphService::nodeOf, spex -> GraphEdgeDto.of(origin, idOf(GraphNodeType.SPEX, spex.getId()),
                        GraphEdgeType.PARTICIPATION, spex.getYear())));

        record Held(Task task, Spex spex, String roles) {
        }

        final List<Held> held = activities.stream()
                .flatMap(activity -> activity.getTaskActivities().stream()
                        .map(taskActivity -> new Held(taskActivity.getTask(),
                                activity.getSpexActivity().getSpex(), rolesOf(taskActivity))))
                .limit(first)
                .toList();

        groups.add(group(GraphEdgeType.FUNCTION, held.size(), held.stream(),
                item -> nodeOf(item.task()),
                item -> GraphEdgeDto.of(idOf(GraphNodeType.TASK, item.task().getId()),
                        idOf(GraphNodeType.SPEX, item.spex().getId()), GraphEdgeType.FUNCTION, item.roles())));

        final List<Tag> tags = List.copyOf(spexare.getTags());

        groups.add(group(GraphEdgeType.TAG, tags.size(), tags.stream().limit(first),
                GraphService::nodeOf, tag -> GraphEdgeDto.of(origin, idOf(GraphNodeType.TAG, tag.getId()),
                        GraphEdgeType.TAG, null)));

        final List<Spexare> partners = Stream.concat(
                        Optional.ofNullable(spexare.getPartner())
                                .map(partner -> spexareRepository.findAll(GraphSpecification.spexareWithId(partner.getId()), BY_ID, BasePermission.READ))
                                .orElseGet(List::of)
                                .stream(),
                        spexareRepository.findAll(GraphSpecification.partnerOf(spexare.getId()), BY_ID, BasePermission.READ).stream())
                .collect(Collectors.toMap(Spexare::getId, partner -> partner, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .limit(first)
                .toList();

        groups.add(group(GraphEdgeType.PARTNER, partners.size(), partners.stream(),
                GraphService::nodeOf, partner -> GraphEdgeDto.of(origin, idOf(GraphNodeType.SPEXARE, partner.getId()),
                        GraphEdgeType.PARTNER, null)));

        return new GraphNeighbourhoodDto(nodeOf(spexare), groups);
    }

    private GraphNeighbourhoodDto neighbourhoodOf(final Spex spex, final int first) {
        final String origin = idOf(GraphNodeType.SPEX, spex.getId());
        final List<GraphGroupDto> groups = new ArrayList<>();
        final CountedWindow<GraphNodeDto> participants =
                spexareWindow(GraphSpecification.spexareInSpex(spex.getId()), firstOnly(first));

        groups.add(new GraphGroupDto(GraphEdgeType.PARTICIPATION, participants.getTotalCount(),
                participants.getContent(),
                participants.getContent().stream()
                        .map(node -> GraphEdgeDto.of(node.id(), origin, GraphEdgeType.PARTICIPATION, node.label()))
                        .toList()));

        final List<SpexCategory> categories = Optional.ofNullable(spex.getDetails().getCategory())
                .map(List::of)
                .orElseGet(List::of);

        groups.add(group(GraphEdgeType.CATEGORY, categories.size(), categories.stream(),
                GraphService::nodeOf, category -> GraphEdgeDto.of(origin, idOf(GraphNodeType.SPEX_CATEGORY, category.getId()),
                        GraphEdgeType.CATEGORY, null)));

        final List<Spex> related = Stream.concat(
                        Optional.ofNullable(spex.getParent()).stream(),
                        spexRepository.findAll(Specification.<Spex>unrestricted()
                                .and((root, _, cb) -> cb.equal(root.get("parent").get("id"), spex.getId())), BY_ID, BasePermission.READ).stream())
                .limit(first)
                .toList();

        groups.add(group(GraphEdgeType.REVIVAL_OF, related.size(), related.stream(),
                GraphService::nodeOf, other -> GraphEdgeDto.of(origin, idOf(GraphNodeType.SPEX, other.getId()),
                        GraphEdgeType.REVIVAL_OF, null)));

        return new GraphNeighbourhoodDto(nodeOf(spex), groups);
    }

    private GraphNeighbourhoodDto neighbourhoodOf(final Task task, final int first) {
        final String origin = idOf(GraphNodeType.TASK, task.getId());
        final List<GraphGroupDto> groups = new ArrayList<>();
        final CountedWindow<GraphNodeDto> participants =
                spexareWindow(GraphSpecification.spexareWithTask(task.getId()), firstOnly(first));

        groups.add(new GraphGroupDto(GraphEdgeType.FUNCTION, participants.getTotalCount(),
                participants.getContent(),
                participants.getContent().stream()
                        .map(node -> GraphEdgeDto.of(node.id(), origin, GraphEdgeType.FUNCTION, null))
                        .toList()));

        final List<TaskCategory> categories = Optional.ofNullable(task.getCategory())
                .map(List::of)
                .orElseGet(List::of);

        groups.add(group(GraphEdgeType.CATEGORY, categories.size(), categories.stream(),
                GraphService::nodeOf, category -> GraphEdgeDto.of(origin, idOf(GraphNodeType.TASK_CATEGORY, category.getId()),
                        GraphEdgeType.CATEGORY, null)));

        return new GraphNeighbourhoodDto(nodeOf(task), groups);
    }

    private GraphNeighbourhoodDto neighbourhoodOf(final Tag tag, final int first) {
        final String origin = idOf(GraphNodeType.TAG, tag.getId());
        final CountedWindow<GraphNodeDto> tagged = spexareWindow(GraphSpecification.spexareWithTag(tag.getId()), firstOnly(first));

        return new GraphNeighbourhoodDto(nodeOf(tag), List.of(new GraphGroupDto(GraphEdgeType.TAG,
                tagged.getTotalCount(), tagged.getContent(),
                tagged.getContent().stream()
                        .map(node -> GraphEdgeDto.of(node.id(), origin, GraphEdgeType.TAG, null))
                        .toList())));
    }

    private GraphNeighbourhoodDto neighbourhoodOf(final SpexCategory category, final int first) {
        final String origin = idOf(GraphNodeType.SPEX_CATEGORY, category.getId());
        final CountedWindow<GraphNodeDto> spex = findNeighbours(GraphNodeType.SPEX_CATEGORY, category.getId(),
                GraphEdgeType.CATEGORY, firstOnly(first));

        return new GraphNeighbourhoodDto(nodeOf(category), List.of(new GraphGroupDto(GraphEdgeType.CATEGORY,
                spex.getTotalCount(), spex.getContent(),
                spex.getContent().stream()
                        .map(node -> GraphEdgeDto.of(node.id(), origin, GraphEdgeType.CATEGORY, null))
                        .toList())));
    }

    private GraphNeighbourhoodDto neighbourhoodOf(final TaskCategory category, final int first) {
        final String origin = idOf(GraphNodeType.TASK_CATEGORY, category.getId());
        final CountedWindow<GraphNodeDto> tasks = findNeighbours(GraphNodeType.TASK_CATEGORY, category.getId(),
                GraphEdgeType.CATEGORY, firstOnly(first));

        return new GraphNeighbourhoodDto(nodeOf(category), List.of(new GraphGroupDto(GraphEdgeType.CATEGORY,
                tasks.getTotalCount(), tasks.getContent(),
                tasks.getContent().stream()
                        .map(node -> GraphEdgeDto.of(node.id(), origin, GraphEdgeType.CATEGORY, null))
                        .toList())));
    }

    private CountedWindow<GraphNodeDto> spexareWindow(final Specification<Spexare> spec, final GraphqlUtil.ScrollRequest scroll) {
        return spexareRepository.findBy(spec, BasePermission.READ, query -> {
            final long total = query.count();

            return CountedWindow.of(query.limit(scroll.limit()).sortBy(BY_ID)
                    .scroll(scroll.positionFor(total)).map(GraphService::nodeOf), total);
        });
    }

}
