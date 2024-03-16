package nu.fgv.register.server.spex;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.acl.PermissionService;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.spex.category.SpexCategoryRepository;
import nu.fgv.register.server.util.FileUtil;
import nu.fgv.register.server.util.filter.FilterParser;
import nu.fgv.register.server.util.filter.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.util.Pair;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spex.SpexSpecification.hasIds;
import static nu.fgv.register.server.spex.SpexSpecification.hasParent;
import static nu.fgv.register.server.spex.SpexSpecification.hasParentIds;
import static nu.fgv.register.server.spex.SpexSpecification.hasYear;
import static nu.fgv.register.server.spex.SpexSpecification.isNotRevival;
import static nu.fgv.register.server.spex.SpexSpecification.isRevival;
import static nu.fgv.register.server.spex.category.SpexCategoryMapper.SPEX_CATEGORY_MAPPER;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_ADMIN_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER_SID;
import static nu.fgv.register.server.util.security.SecurityUtil.toObjectIdentity;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexService {

    private final SpexRepository repository;
    private final SpexDetailsRepository detailsRepository;
    private final SpexCategoryRepository categoryRepository;
    private final PermissionService permissionService;

    public List<SpexDto> findAll(final Sort sort) {
        return repository
                .findAll(isNotRevival(), sort, BasePermission.READ)
                .stream().map(SPEX_MAPPER::toDto)
                .toList();
    }

    public Page<SpexDto> find(final String filter, final Pageable pageable) {
        return hasText(filter) ?
                repository
                        .findAll(SpecificationsBuilder.<Spex>builder().build(FilterParser.parse(filter), SpexSpecification::new), pageable, BasePermission.READ)
                        .map(SPEX_MAPPER::toDto) :
                repository
                        .findAll(pageable, BasePermission.READ)
                        .map(SPEX_MAPPER::toDto);
    }

    public Optional<SpexDto> findById(final Long id) {
        return repository
                .findById0(id)
                .map(SPEX_MAPPER::toDto);
    }

    public List<SpexDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findAll(hasIds(ids), sort, BasePermission.READ)
                .stream()
                .map(SPEX_MAPPER::toDto)
                .toList();
    }

    public List<SpexDto> findRevivalsByParentIds(final List<Long> parentIds, final Sort sort) {
        return repository
                .findAll(hasParentIds(parentIds), sort, BasePermission.READ)
                .stream()
                .map(SPEX_MAPPER::toDto)
                .toList();
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public SpexDto create(final SpexCreateDto dto) {
        return Optional.of(SPEX_MAPPER.toModel(dto))
                .map(model -> {
                    detailsRepository.save(model.getDetails());
                    final Spex spex = repository.save(model);
                    final ObjectIdentity oid = toObjectIdentity(Spex.class, spex.getId());

                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                    permissionService.grantPermission(oid, BasePermission.DELETE, ROLE_ADMIN_SID);
                    permissionService.grantPermission(oid, BasePermission.READ, ROLE_USER_SID);

                    return SPEX_MAPPER.toDto(spex);
                })
                .orElse(null);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public Optional<SpexDto> update(final SpexUpdateDto dto) {
        return partialUpdate(dto);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public Optional<SpexDto> partialUpdate(final SpexUpdateDto dto) {
        return repository
                .findById0(dto.getId())
                .map(spex -> {
                    SPEX_MAPPER.toPartialModel(dto, spex);
                    return spex;
                })
                .map(spex -> {
                    detailsRepository.save(spex.getDetails());
                    return repository.save(spex);
                })
                .map(SPEX_MAPPER::toDto);
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public void deleteById(final Long id) {
        repository
                .findById0(id)
                .ifPresent(spex -> {
                    repository.findAll(hasParent(spex)).forEach(revival -> {
                        repository.deleteById(revival.getId());
                        permissionService.deleteAcl(toObjectIdentity(Spex.class, revival.getId()));
                    });
                    repository.deleteById(spex.getId());
                    permissionService.deleteAcl(toObjectIdentity(Spex.class, id));
                    detailsRepository.deleteById(spex.getDetails().getId());
                });
    }

    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public Optional<SpexDto> savePoster(final Long spexId, final byte[] poster, final String contentType) {
        return repository
                .findById0(spexId)
                .map(spex -> {
                    spex.getDetails().setPoster(poster);
                    spex.getDetails().setPosterContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(poster));
                    detailsRepository.save(spex.getDetails());
                    return SPEX_MAPPER.toDto(spex);
                });
    }

    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public Optional<SpexDto> deletePoster(final Long spexId) {
        return repository
                .findById0(spexId)
                .map(spex -> {
                    spex.getDetails().setPoster(null);
                    spex.getDetails().setPosterContentType(null);
                    detailsRepository.save(spex.getDetails());
                    return SPEX_MAPPER.toDto(spex);
                });
    }

    public Optional<Pair<byte[], String>> getPoster(final Long spexId) {
        return repository
                .findById0(spexId)
                .map(Spex::getDetails)
                .filter(details -> details.getPoster() != null && hasText(details.getPosterContentType()))
                .map(details -> Pair.of(details.getPoster(), details.getPosterContentType()));
    }

    public Optional<SpexDto> findRevivalById(final Long spexId, final Long id) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById0(id)
                    .filter(revival -> revival.getParent() != null && revival.getParent().getId().equals(spexId))
                    .map(SPEX_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    public Page<SpexDto> findRevivals(final Pageable pageable) {
        return repository
                .findAll(isRevival(), pageable, BasePermission.READ)
                .map(SPEX_MAPPER::toDto);
    }

    public Page<SpexDto> findRevivalsByParent(final Long spexId, final Pageable pageable) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById0(spexId)
                    .map(parent -> repository
                            .findAll(hasParent(parent), pageable, BasePermission.READ)
                            .map(SPEX_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public Optional<SpexDto> addRevival(final Long spexId, final String year) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById0(spexId)
                    .filter(parent -> !repository.exists(hasParent(parent).and(hasYear(year))))
                    .map(parent -> {
                        final Spex spex = new Spex();
                        spex.setDetails(parent.getDetails());
                        spex.setParent(parent);
                        spex.setYear(year);

                        final Spex revival = repository.save(spex);
                        final ObjectIdentity oid = toObjectIdentity(Spex.class, revival.getId());

                        permissionService.grantPermission(oid, BasePermission.READ, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                        permissionService.grantPermission(oid, BasePermission.WRITE, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                        permissionService.grantPermission(oid, BasePermission.DELETE, ROLE_ADMIN_SID, ROLE_EDITOR_SID);
                        permissionService.grantPermission(oid, BasePermission.READ, ROLE_USER_SID);

                        return revival;
                    })
                    .map(SPEX_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN') or hasRole('spexregister_EDITOR')")
    public boolean deleteRevival(final Long spexId, final String year) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById0(spexId)
                    .filter(parent -> repository.exists(hasParent(parent).and(hasYear(year))))
                    .flatMap(parent -> repository.findOne(hasParent(parent).and(hasYear(year))))
                    .map(revival -> {
                        permissionService.deleteAcl(toObjectIdentity(Spex.class, revival.getId()));
                        repository.deleteById(revival.getId());
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    public Optional<SpexCategoryDto> findCategoryBySpex(final Long spexId) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById0(spexId)
                    .map(spex -> spex.getDetails().getCategory())
                    .map(SPEX_CATEGORY_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean addCategory(final Long spexId, final Long id) {
        if (doSpexAndCategoryExist(spexId, id)) {
            return repository
                    .findById0(spexId)
                    .map(spex -> categoryRepository
                            .findById(id)
                            .map(category -> {
                                spex.getDetails().setCategory(category);
                                detailsRepository.save(spex.getDetails());
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s and/or category %s do not exist", spexId, id));
        }
    }

    @PreAuthorize("hasRole('spexregister_ADMIN')")
    public boolean removeCategory(final Long spexId) {
        if (doesSpexExist(spexId)) {
            return repository
                    .findById0(spexId)
                    .filter(spex -> spex.getDetails().getCategory() != null)
                    .map(spex -> {
                        spex.getDetails().setCategory(null);
                        detailsRepository.save(spex.getDetails());
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spex %s does not exist", spexId));
        }
    }

    private boolean doesSpexExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doSpexAndCategoryExist(final Long spexId, final Long categoryId) {
        return doesSpexExist(spexId) && categoryRepository.existsById(categoryId);
    }
}
