package nu.fgv.register.server.news;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.news.NewsMapper.NEWS_MAPPER;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleFromAfterYesterday;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleToAfterToday;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleToBeforeToday;
import static nu.fgv.register.server.news.NewsSpecification.hasVisibleToToday;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class NewsService {

    private final NewsRepository repository;
    private final PermissionService permissionService;

    public List<NewsDto> findAll(final Sort sort) {
        return repository
                .findAll(sort, BasePermission.READ)
                .stream()
                .map(NEWS_MAPPER::toDto)
                .toList();
    }

    public Page<NewsDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<News>builder().build(FilterParser.parse(filter), NewsSpecification::new), pageable, BasePermission.READ)
                        .map(NEWS_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(NEWS_MAPPER::toDto);
    }

    public Optional<NewsDto> findById(final Long id) {
        return findById0(id)
                .map(NEWS_MAPPER::toDto);
    }

    @PostAuthorize("!returnObject.isEmpty() ? hasPermission(returnObject.get(), 'READ') : returnObject")
    private Optional<News> findById0(final Long id) {
        return repository
                .findById(id);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EDITOR')")
    public NewsDto create(final NewsCreateDto dto) {
        return Optional.of(NEWS_MAPPER.toModel(dto))
                .map(repository::save)
                .map(news -> {
                    final ObjectIdentity oid = toObjectIdentity(News.class, news.getId());

                    permissionService.grantPermission(oid, BasePermission.ADMINISTRATION, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    if (Boolean.TRUE.equals(news.getPublished())) {
                        permissionService.grantPermission(oid, ROLE_USER_SID, BasePermission.READ);
                    }

                    return NEWS_MAPPER.toDto(news);
                })
                .orElse(null);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EDITOR')")
    public Optional<NewsDto> update(final NewsUpdateDto dto) {
        return partialUpdate(dto);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EDITOR')")
    public Optional<NewsDto> partialUpdate(final NewsUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(news -> {
                    NEWS_MAPPER.toPartialModel(dto, news);
                    return news;
                })
                .map(repository::save)
                .map(news -> {
                    final ObjectIdentity oid = toObjectIdentity(News.class, news.getId());

                    if (Boolean.TRUE.equals(news.getPublished())) {
                        permissionService.grantPermission(oid, ROLE_USER_SID, BasePermission.READ);
                    } else {
                        permissionService.revokePermission(oid, ROLE_USER_SID, BasePermission.READ);
                    }

                    return news;
                })
                .map(NEWS_MAPPER::toDto);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EDITOR')")
    public void deleteById(final Long id) {
        repository.deleteById(id);
        permissionService.deleteAcl(toObjectIdentity(News.class, id));
    }

    @Scheduled(cron = "${spexregister.jobs.publish-unpublish-news.cron-expression}")
    public void publishUnpublishNews() {
        repository
                .findAll(hasVisibleToBeforeToday())
                .stream()
                .peek(news -> news.setPublished(false)) // NOSONAR
                .map(repository::save)
                .forEach(news -> {
                    final ObjectIdentity oid = toObjectIdentity(News.class, news.getId());

                    permissionService.revokePermission(oid, ROLE_USER_SID, BasePermission.READ);
                });

        repository
                .findAll(hasVisibleFromAfterYesterday().and(hasVisibleToToday().or(hasVisibleToAfterToday())))
                .stream()
                .peek(news -> news.setPublished(true)) // NOSONAR
                .map(repository::save)
                .forEach(news -> {
                    final ObjectIdentity oid = toObjectIdentity(News.class, news.getId());

                    permissionService.grantPermission(oid, ROLE_USER_SID, BasePermission.READ);
                });
    }
}
