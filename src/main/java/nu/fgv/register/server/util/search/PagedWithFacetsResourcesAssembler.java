package nu.fgv.register.server.util.search;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.util.UriComponentsBuilder.fromUri;

public class PagedWithFacetsResourcesAssembler<T> implements RepresentationModelAssembler<PageWithFacets<T>, PagedWithFacetsModel<EntityModel<T>>> {

    private final HateoasPageableHandlerMethodArgumentResolver pageableResolver;
    private final Optional<UriComponents> baseUri;
    private final EmbeddedWrappers wrappers = new EmbeddedWrappers(false);

    private boolean forceFirstAndLastRels = false;
    private @Nullable MethodParameter parameter;

    public PagedWithFacetsResourcesAssembler(@Nullable final HateoasPageableHandlerMethodArgumentResolver resolver,
                                             @Nullable final UriComponents baseUri) {
        this(resolver, Optional.ofNullable(baseUri), null);
    }

    private PagedWithFacetsResourcesAssembler(@Nullable final HateoasPageableHandlerMethodArgumentResolver resolver,
                                              final Optional<UriComponents> baseUri,
                                              @Nullable final MethodParameter parameter) {
        this.pageableResolver = resolver == null ? new HateoasPageableHandlerMethodArgumentResolver() : resolver;
        this.baseUri = baseUri;
        this.parameter = parameter;
    }

    public void setForceFirstAndLastRels(final boolean forceFirstAndLastRels) {
        this.forceFirstAndLastRels = forceFirstAndLastRels;
    }

    public PagedWithFacetsResourcesAssembler<T> withParameter(@Nullable final MethodParameter parameter) {
        return new PagedWithFacetsResourcesAssembler<>(pageableResolver, baseUri, parameter);
    }

    @Override
    public PagedWithFacetsModel<EntityModel<T>> toModel(final PageWithFacets<T> entity) {
        return toModel(entity, EntityModel::of);
    }

    public PagedWithFacetsModel<EntityModel<T>> toModel(final PageWithFacets<T> page, final Link selfLink) {
        return toModel(page, EntityModel::of, selfLink);
    }

    public <R extends RepresentationModel<?>> PagedWithFacetsModel<R> toModel(final PageWithFacets<T> page,
                                                                              final RepresentationModelAssembler<T, R> assembler) {
        return createModel(page, assembler, Optional.empty());
    }

    public <R extends RepresentationModel<?>> PagedWithFacetsModel<R> toModel(final PageWithFacets<T> page,
                                                                              final RepresentationModelAssembler<T, R> assembler,
                                                                              final Link link) {
        Assert.notNull(link, "Link must not be null");

        return createModel(page, assembler, Optional.of(link));
    }

    public PagedWithFacetsModel<?> toEmptyModel(final PageWithFacets<?> page, final Class<?> type) {
        return toEmptyModel(page, type, Optional.empty());
    }

    public PagedWithFacetsModel<?> toEmptyModel(final PageWithFacets<?> page, final Class<?> type, final Link link) {
        return toEmptyModel(page, type, Optional.of(link));
    }

    private PagedWithFacetsModel<?> toEmptyModel(final PageWithFacets<?> page, final Class<?> type, final Optional<Link> link) {
        Assert.notNull(page, "Page must not be null");
        Assert.isTrue(!page.hasContent(), "Page must not have any content");
        Assert.notNull(type, "Type must not be null");
        Assert.notNull(link, "Link must not be null");

        final PagedWithFacetsModel.PageMetadata metadata = asPageMetadata(page);

        final EmbeddedWrapper wrapper = wrappers.emptyCollectionOf(type);
        final List<EmbeddedWrapper> embedded = Collections.singletonList(wrapper);

        return addPaginationLinks(PagedWithFacetsModel.of(embedded, metadata, Collections.emptyList()), page, link);
    }

    protected <R extends RepresentationModel<?>, S> PagedWithFacetsModel<R> createPagedModel(final List<R> resources,
                                                                                             final PagedWithFacetsModel.PageMetadata metadata,
                                                                                             final PageWithFacets<S> page) {
        Assert.notNull(resources, "Content resources must not be null");
        Assert.notNull(metadata, "PageMetadata must not be null");
        Assert.notNull(page, "Page must not be null");

        return PagedWithFacetsModel.of(resources, metadata, page.getFacets());
    }

    private <S, R extends RepresentationModel<?>> PagedWithFacetsModel<R> createModel(final PageWithFacets<S> page,
                                                                                      final RepresentationModelAssembler<S, R> assembler,
                                                                                      final Optional<Link> link) {
        Assert.notNull(page, "Page must not be null");
        Assert.notNull(assembler, "ResourceAssembler must not be null");

        final List<R> resources = new ArrayList<>(page.getNumberOfElements());

        for (final S element : page) {
            resources.add(assembler.toModel(element));
        }

        final PagedWithFacetsModel<R> resource = createPagedModel(resources, asPageMetadata(page), page);

        return addPaginationLinks(resource, page, link);
    }

    private <R> PagedWithFacetsModel<R> addPaginationLinks(final PagedWithFacetsModel<R> resources, final PageWithFacets<?> page, final Optional<Link> link) {
        final UriTemplate base = getUriTemplate(link);
        final boolean isNavigable = page.hasPrevious() || page.hasNext();

        if (isNavigable || forceFirstAndLastRels) {
            resources.add(createLink(base, PageRequest.of(0, page.getSize(), page.getSort()), IanaLinkRelations.FIRST));
        }

        if (page.hasPrevious()) {
            resources.add(createLink(base, page.previousPageable(), IanaLinkRelations.PREV));
        }

        final Link selfLink = link.map(Link::withSelfRel)
                .orElseGet(() -> createLink(base, page.getPageable(), IanaLinkRelations.SELF));

        resources.add(selfLink);

        if (page.hasNext()) {
            resources.add(createLink(base, page.nextPageable(), IanaLinkRelations.NEXT));
        }

        if (isNavigable || forceFirstAndLastRels) {
            final int lastIndex = page.getTotalPages() == 0 ? 0 : page.getTotalPages() - 1;

            resources.add(createLink(base, PageRequest.of(lastIndex, page.getSize(), page.getSort()), IanaLinkRelations.LAST));
        }

        return resources;
    }

    private UriTemplate getUriTemplate(final Optional<Link> baseLink) {
        return UriTemplate.of(baseLink.map(Link::getHref).orElseGet(this::baseUriOrCurrentRequest));
    }

    private Link createLink(final UriTemplate base, final Pageable pageable, final LinkRelation relation) {
        final UriComponentsBuilder builder = fromUri(base.expand());
        pageableResolver.enhance(builder, parameter, pageable);

        return Link.of(UriTemplate.of(builder.build().toString()), relation);
    }

    private PagedWithFacetsModel.PageMetadata asPageMetadata(final PageWithFacets<?> page) {
        Assert.notNull(page, "Page must not be null");

        // TODO: pageableResolver.isOneIndexedParameters() is not reachable as it is protected, any workaround?
        // final int number = pageableResolver.isOneIndexedParameters() ? page.getNumber() + 1 : page.getNumber();
        final int number = page.getNumber();

        return new PagedWithFacetsModel.PageMetadata(page.getSize(), number, page.getTotalElements(), page.getTotalPages());
    }

    private String baseUriOrCurrentRequest() {
        return baseUri.map(Object::toString).orElseGet(PagedWithFacetsResourcesAssembler::currentRequest);
    }

    private static String currentRequest() {
        return ServletUriComponentsBuilder.fromCurrentRequest().build().toString();
    }

}
