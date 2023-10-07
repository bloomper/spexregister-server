package nu.fgv.register.server.util.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PagedWithFacetsModel<T> extends CollectionModel<T> {

    public static PagedWithFacetsModel<?> NO_PAGE = new PagedWithFacetsModel<>();

    private final PageMetadata metadata;
    private final @Nullable ResolvableType fallbackType;
    private final Collection<Facet> facets;

    protected PagedWithFacetsModel() {
        this(new ArrayList<>(), null, new ArrayList<>());
    }

    protected PagedWithFacetsModel(final Collection<T> content, @Nullable final PageMetadata metadata, final Collection<Facet> facets) {
        this(content, metadata, Links.NONE, facets);
    }

    protected PagedWithFacetsModel(final Collection<T> content, @Nullable final PageMetadata metadata, final Iterable<Link> links, final Collection<Facet> facets) {
        this(content, metadata, links, null, facets);
    }

    protected PagedWithFacetsModel(final Collection<T> content, @Nullable final PageMetadata metadata, final Iterable<Link> links, @Nullable final ResolvableType fallbackType, final Collection<Facet> facets) {
        super(content, links, fallbackType);

        this.metadata = metadata;
        this.fallbackType = fallbackType;
        this.facets = facets;
    }

    public static <T> PagedWithFacetsModel<T> empty() {
        return empty(Collections.emptyList());
    }

    public static <T> PagedWithFacetsModel<T> empty(final Class<T> fallbackElementType, final Class<?> generics) {
        return empty(ResolvableType.forClassWithGenerics(fallbackElementType, generics));
    }

    public static <T> PagedWithFacetsModel<T> empty(final ParameterizedTypeReference<T> fallbackElementType) {
        return empty(ResolvableType.forType(fallbackElementType));
    }

    public static <T> PagedWithFacetsModel<T> empty(final ResolvableType fallbackElementType) {
        return new PagedWithFacetsModel<>(Collections.emptyList(), null, Collections.emptyList(), fallbackElementType, Collections.emptyList());
    }

    public static <T> PagedWithFacetsModel<T> empty(final Link... links) {
        return empty(null, links);
    }

    public static <T> PagedWithFacetsModel<T> empty(final Iterable<Link> links) {
        return empty(null, links);
    }

    public static <T> PagedWithFacetsModel<T> empty(@Nullable final PageMetadata metadata) {
        return empty(metadata, Collections.emptyList());
    }

    public static <T> PagedWithFacetsModel<T> empty(@Nullable final PageMetadata metadata, final Class<?> fallbackType, final Class<?>... generics) {
        Assert.notNull(fallbackType, "Fallback type must not be null!");
        Assert.notNull(generics, "Generics must not be null!");

        return empty(metadata, ResolvableType.forClassWithGenerics(fallbackType, generics));
    }

    public static <T> PagedWithFacetsModel<T> empty(@Nullable final PageMetadata metadata, final ParameterizedTypeReference<T> fallbackType) {
        Assert.notNull(fallbackType, "Fallback type must not be null!");

        return empty(metadata, ResolvableType.forType(fallbackType));
    }

    public static <T> PagedWithFacetsModel<T> empty(@Nullable final PageMetadata metadata, final ResolvableType fallbackType) {
        Assert.notNull(fallbackType, "Fallback type must not be null!");

        return new PagedWithFacetsModel<>(Collections.emptyList(), metadata, Collections.emptyList(), fallbackType, Collections.emptyList());
    }

    public static <T> PagedWithFacetsModel<T> empty(@Nullable final PageMetadata metadata, final Link... links) {
        return empty(Arrays.asList(links));
    }

    public static <T> PagedWithFacetsModel<T> empty(@Nullable final PageMetadata metadata, final Iterable<Link> links) {
        return of(Collections.emptyList(), metadata, Collections.emptyList(), links);
    }

    public static <T> PagedWithFacetsModel<T> of(final Collection<T> content, @Nullable final PageMetadata metadata, final Collection<Facet> facets) {
        return new PagedWithFacetsModel<>(content, metadata, facets);
    }

    public static <T> PagedWithFacetsModel<T> of(Collection<T> content, @Nullable PageMetadata metadata, final Collection<Facet> facets, Link... links) {
        return new PagedWithFacetsModel<>(content, metadata, List.of(links), facets);
    }

    public static <T> PagedWithFacetsModel<T> of(final Collection<T> content, @Nullable final PageMetadata metadata, final Collection<Facet> facets, final Iterable<Link> links) {
        return new PagedWithFacetsModel<>(content, metadata, links, facets);
    }

    @JsonProperty("_facets")
    public Collection<Facet> getFacets() {
        return facets;
    }

    @JsonProperty("page")
    @Nullable
    public PageMetadata getMetadata() {
        return metadata;
    }

    @SuppressWarnings("unchecked")
    public static <T extends EntityModel<S>, S> PagedWithFacetsModel<T> wrap(final Iterable<S> content, final PageMetadata metadata, final Collection<Facet> facets) {
        Assert.notNull(content, "Content must not be null!");

        final ArrayList<T> resources = new ArrayList<>();

        for (final S element : content) {
            resources.add((T) EntityModel.of(element));
        }

        return PagedWithFacetsModel.of(resources, metadata, facets);
    }

    @JsonIgnore
    public Optional<Link> getNextLink() {
        return getLink(IanaLinkRelations.NEXT);
    }

    @JsonIgnore
    public Optional<Link> getPreviousLink() {
        return getLink(IanaLinkRelations.PREV);
    }

    @Override
    public PagedWithFacetsModel<T> withFallbackType(final Class<? super T> type, final Class<?>... generics) {
        return withFallbackType(ResolvableType.forClassWithGenerics(type, generics));
    }

    @Override
    public PagedWithFacetsModel<T> withFallbackType(final ParameterizedTypeReference<?> type) {
        return withFallbackType(ResolvableType.forType(type));
    }

    @Override
    public PagedWithFacetsModel<T> withFallbackType(final ResolvableType type) {
        return new PagedWithFacetsModel<>(getContent(), metadata, getLinks(), type, getFacets());
    }

    @Override
    public String toString() {
        return String.format("PagedWithFacetsModel { content: %s, fallbackType: %s, metadata: %s, links: %s, facets: %s }", getContent(), fallbackType, metadata, getLinks(), getFacets());
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }

        final PagedWithFacetsModel<?> that = (PagedWithFacetsModel<?>) obj;

        return Objects.equals(this.metadata, that.metadata) && super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(metadata);
    }

    @Getter
    public static class PageMetadata {

        @JsonProperty
        private long size;
        @JsonProperty
        private long totalElements;
        @JsonProperty
        private long totalPages;
        @JsonProperty
        private long number;

        protected PageMetadata() {
        }

        public PageMetadata(final long size, final long number, final long totalElements, final long totalPages) {
            Assert.isTrue(size > -1, "Size must not be negative!");
            Assert.isTrue(number > -1, "Number must not be negative!");
            Assert.isTrue(totalElements > -1, "Total elements must not be negative!");
            Assert.isTrue(totalPages > -1, "Total pages must not be negative!");

            this.size = size;
            this.number = number;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        public PageMetadata(final long size, final long number, final long totalElements) {
            this(size, number, totalElements, size == 0 ? 0 : (long) Math.ceil((double) totalElements / (double) size));
        }

        @Override
        public String toString() {
            return String.format("Metadata { number: %d, total pages: %d, total elements: %d, size: %d }", number, totalPages, totalElements, size);
        }

        @Override
        public boolean equals(@Nullable final Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || !obj.getClass().equals(getClass())) {
                return false;
            }

            final PageMetadata that = (PageMetadata) obj;

            return this.number == that.number && this.size == that.size && this.totalElements == that.totalElements && this.totalPages == that.totalPages;
        }

        @Override
        public int hashCode() {
            int result = 17;

            result += 31 * (int) (this.number ^ this.number >>> 32);
            result += 31 * (int) (this.size ^ this.size >>> 32);
            result += 31 * (int) (this.totalElements ^ this.totalElements >>> 32);
            result += 31 * (int) (this.totalPages ^ this.totalPages >>> 32);

            return result;
        }
    }
}
