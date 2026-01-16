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
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.error.ResourceNotFoundException;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import nu.fgv.register.server.util.security.RequiresAdminOrEditor;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static nu.fgv.register.server.news.NewsMapper.NEWS_MAPPER;
import static nu.fgv.register.server.news.NewsSpecification.NO_FILTER;
import static nu.fgv.register.server.news.NewsSpecification.hasIds;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleFromAfterYesterday;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleToAfterToday;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleToBeforeToday;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleToToday;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.runAsSystem;
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
public class NewsService {

    private final NewsRepository repository;
    private final PermissionService permissionService;

    @RequiresAdminOrEditorOrUser
    public Window<NewsDto> find(final String filter, final int limit, final Sort sort, final ScrollPosition scrollPosition) {
        return hasText(filter) ?
                repository
                        .findBy(SpecificationsBuilder.<News>builder().build(FilterParser.parse(filter), NewsSpecification::new), BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(NEWS_MAPPER::toDto) :
                repository
                        .findBy(NO_FILTER, BasePermission.READ, query -> query
                                .limit(limit)
                                .sortBy(sort)
                                .scroll(scrollPosition))
                        .map(NEWS_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public Page<NewsDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<News>builder().build(FilterParser.parse(filter), NewsSpecification::new), pageable, BasePermission.READ)
                        .map(NEWS_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(NEWS_MAPPER::toDto);
    }

    @RequiresAdminOrEditorOrUser
    public NewsDto findById(final Long id) {
        return repository.findById0(id)
                .map(NEWS_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(News.class, id));
    }

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

            return repository.streamAll(spec, sort, BasePermission.READ)
                    .iterator();
        };
    }

    @RequiresAdminOrEditor
    public NewsDto create(final NewsCreateDto dto) {
        return Optional.of(NEWS_MAPPER.toModel(dto))
                .map(repository::save)
                .map(news -> {
                    final ObjectIdentity oid = toObjectIdentity(News.class, news.getId());

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.DELETE, ROLE_EDITOR_SID);
                    if (news.getPublished()) {
                        permissionService.grantPermission(oid, ROLE_USER_SID, BasePermission.READ);
                    }

                    return NEWS_MAPPER.toDto(news);
                })
                .orElseThrow(() -> new InternalErrorException("Could not create news"));
    }

    @RequiresAdminOrEditor
    public NewsDto update(final NewsUpdateDto dto) {
        return partialUpdate(dto);
    }

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
                .map(news -> {
                    final ObjectIdentity oid = toObjectIdentity(News.class, news.getId());

                    if (news.getPublished()) {
                        permissionService.grantPermission(oid, ROLE_USER_SID, BasePermission.READ);
                    } else {
                        permissionService.revokePermission(oid, ROLE_USER_SID, BasePermission.READ);
                    }

                    return news;
                })
                .map(NEWS_MAPPER::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(News.class, dto.id()));
    }

    @RequiresAdminOrEditor
    public void deleteById(final Long id) {
        if (doesNewsExist(id)) {
            repository.findById0(id)
                    .map(permissionService::checkDeletePermission)
                    .ifPresent(news -> {
                        permissionService.deleteAcl(toObjectIdentity(News.class, id));
                        repository.delete(news);
                    });
        } else {
            throw new ResourceNotFoundException(News.class, id);
        }
    }

    @Scheduled(cron = "${spexregister.jobs.publish-unpublish-news.cron-expression}")
    public void publishAndUnpublishNews() {
        log.info("Starting scheduled news publish/unpublish job");
        runAsSystem(() -> {
            final AtomicInteger unpublishedCount = new AtomicInteger();
            final AtomicInteger publishedCount = new AtomicInteger();

            repository
                    .findAll(hasVisibleToBeforeToday())
                    .stream()
                    .peek(news -> news.setPublished(false)) // NOSONAR
                    .map(repository::save)
                    .forEach(news -> {
                        final ObjectIdentity oid = toObjectIdentity(News.class, news.getId());

                        permissionService.revokePermission(oid, ROLE_USER_SID, BasePermission.READ);
                        unpublishedCount.incrementAndGet();
                    });

            repository
                    .findAll(hasVisibleFromAfterYesterday().and(hasVisibleToToday().or(hasVisibleToAfterToday())))
                    .stream()
                    .peek(news -> news.setPublished(true)) // NOSONAR
                    .map(repository::save)
                    .forEach(news -> {
                        final ObjectIdentity oid = toObjectIdentity(News.class, news.getId());

                        permissionService.grantPermission(oid, ROLE_USER_SID, BasePermission.READ);
                        publishedCount.incrementAndGet();
                    });
            log.info("Finished news publish/unpublish job (published: {}, unpublished: {})", publishedCount.get(), unpublishedCount.get());
        });
    }

    private boolean doesNewsExist(final Long id) {
        return repository.findById0(id).isPresent();
    }
}
