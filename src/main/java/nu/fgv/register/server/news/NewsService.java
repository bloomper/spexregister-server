/*
 * Copyright 2024 the original author or authors.
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

package nu.fgv.register.server.news;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.graphql.CountedWindow;
import nu.fgv.register.server.util.graphql.GraphqlUtil.ScrollRequest;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import nu.fgv.register.server.security.policy.Permission;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static nu.fgv.register.server.news.NewsMapper.NEWS_MAPPER;
import static nu.fgv.register.server.news.NewsSpecification.NO_FILTER;
import static nu.fgv.register.server.news.NewsSpecification.hasIds;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleFromTodayOrEarlier;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleToAfterToday;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleToBeforeToday;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleToToday;
import static nu.fgv.register.server.news.NewsSpecification.isPublished;
import static nu.fgv.register.server.util.security.SecurityUtil.runAsSystem;
import static org.springframework.util.StringUtils.hasText;

/**
 * News. Users read only what is published; editors manage it all.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class NewsService {

    private final NewsRepository repository;
    private final PermissionService permissionService;

    /**
     * The news the caller may read that match the filter.
     *
     * @param filter a filter expression, or blank for all
     * @param scroll the window to read
     * @param sort   the order
     * @return the window with the total count
     */
    @RequiresAdminOrEditorOrUser
    public CountedWindow<NewsDto> find(final String filter, final ScrollRequest scroll, final Sort sort) {
        return repository
                .findBy(hasText(filter) ?
                                SpecificationsBuilder.<News>builder().build(FilterParser.parse(filter), NewsSpecification::new) :
                                NO_FILTER,
                        Permission.READ, query -> {
                            final long total = query.count();

                            return CountedWindow.of(query
                                    .limit(scroll.limit())
                                    .sortBy(sort)
                                    .scroll(scroll.positionFor(total))
                                    .map(NEWS_MAPPER::toDto), total);
                        });
    }

    /**
     * The news the caller may read that match the filter, for the REST API.
     *
     * @param filter   a filter expression, or blank for all
     * @param pageable the page to read
     * @return the page
     */
    @RequiresAdminOrEditorOrUser
    public Page<NewsDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<News>builder().build(FilterParser.parse(filter), NewsSpecification::new), pageable, Permission.READ)
                        .map(NEWS_MAPPER::toDto) :
                repository
                        .findAll(pageable, Permission.READ)
                        .map(NEWS_MAPPER::toDto);
    }

    /**
     * @param id the news item id
     * @return the news item
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such news item
     */
    @RequiresAdminOrEditorOrUser
    public NewsDto findById(final Long id) {
        return repository.findById0(id)
                .map(NEWS_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(News.class, id));
    }

    /**
     * @param id the news item id
     * @return whether the news item exists
     * @throws org.springframework.security.access.AccessDeniedException when it exists but may not be read
     */
    @RequiresAdminOrEditorOrUser
    public boolean exists(final Long id) {
        return repository
                .findById0(id)
                .isPresent();
    }

    /**
     * The news to export, streamed when iterated, so it must be consumed inside a transaction.
     *
     * @param ids    the news to export, or empty to use the filter
     * @param filter a filter expression when no ids are given, or blank for all
     * @param sort   the order
     * @return the news the caller may read
     */
    @RequiresAdminOrEditorOrUser
    public Iterable<News> streamByIds(final List<Long> ids, final String filter, final Sort sort) {
        return () -> {
            final Specification<News> spec;

            if (!ids.isEmpty()) {
                spec = hasIds(ids);
            } else if (hasText(filter)) {
                spec = SpecificationsBuilder.<News>builder()
                        .build(FilterParser.parse(filter), NewsSpecification::new);
            } else {
                spec = null;
            }

            return repository.streamAll(spec, sort, Permission.READ)
                    .iterator();
        };
    }

    /**
     * @param dto the new news item
     * @return the created news item
     */
    @RequiresAdminOrEditor
    public NewsDto create(final NewsCreateDto dto) {
        return Optional.of(NEWS_MAPPER.toModel(dto))
                .map(repository::save)
                .map(news -> {
                    return NEWS_MAPPER.toDto(news);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create news"));
    }

    /**
     * The same as {@link #partialUpdate}.
     *
     * @param dto the news item id and new values
     * @return the updated news item
     */
    @RequiresAdminOrEditor
    public NewsDto update(final NewsUpdateDto dto) {
        return partialUpdate(dto);
    }

    /**
     * Changes the values given; null values are left unchanged.
     *
     * @param dto the news item id and new values
     * @return the updated news item
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such news item
     */
    @RequiresAdminOrEditor
    public NewsDto partialUpdate(final NewsUpdateDto dto) {
        return repository
                .findById0(dto.id())
                .map(permissionService::checkWritePermission)
                .map(news -> {
                    NEWS_MAPPER.toPartialModel(dto, news);
                    return news;
                })
                .map(repository::save)
                .map(NEWS_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(News.class, dto.id()));
    }

    /**
     * @param id the news item id
     * @throws nu.fgv.register.server.util.error.ResourceNotFoundException when there is no such news item
     */
    @RequiresAdminOrEditor
    public void deleteById(final Long id) {
        if (doesNewsExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(news -> {
                        repository.delete(news);
                    });
        } else {
            throw new ResourceNotFoundException(News.class, id);
        }
    }

    /**
     * Publishes the news whose visibility period has started and unpublishes the news whose period has ended, on the
     * {@code spexregister.jobs.publish-unpublish-news.cron-expression} schedule.
     */
    @Scheduled(cron = "${spexregister.jobs.publish-unpublish-news.cron-expression}")
    public void publishAndUnpublishNews() {
        log.info("Starting scheduled news publish/unpublish job");
        runAsSystem(() -> {
            final AtomicInteger unpublishedCount = new AtomicInteger();
            final AtomicInteger publishedCount = new AtomicInteger();

            repository
                    .findAll(hasVisibleToBeforeToday().and(isPublished(true)))
                    .stream()
                    .peek(news -> news.setPublished(false)) // NOSONAR
                    .map(repository::save)
                    .forEach(news -> {
                        unpublishedCount.incrementAndGet();
                    });

            repository
                    .findAll(isPublished(false).and(hasVisibleFromTodayOrEarlier()).and(hasVisibleToToday().or(hasVisibleToAfterToday())))
                    .stream()
                    .peek(news -> news.setPublished(true)) // NOSONAR
                    .map(repository::save)
                    .forEach(news -> {
                        publishedCount.incrementAndGet();
                    });
            log.info("Finished news publish/unpublish job (published: {}, unpublished: {})", publishedCount.get(), unpublishedCount.get());
        });
    }

    private boolean doesNewsExist(final Long id) {
        return repository.findById0(id).isPresent();
    }
}
