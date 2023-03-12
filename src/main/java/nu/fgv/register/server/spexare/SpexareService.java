package nu.fgv.register.server.spexare;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.FileUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexareService {

    private final SpexareRepository repository;

    public List<SpexareDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream()
                .map(SPEXARE_MAPPER::toDto)
                .collect(Collectors.toList());
    }

    public Page<SpexareDto> find(final Pageable pageable) {
        return repository
                .findAll(pageable)
                .map(SPEXARE_MAPPER::toDto);
    }

    public Optional<SpexareDto> findById(final Long id) {
        return repository
                .findById(id)
                .map(SPEXARE_MAPPER::toDto);
    }

    public List<SpexareDto> findByIds(final List<Long> ids, final Sort sort) {
        return repository
                .findByIds(ids, sort)
                .stream()
                .map(SPEXARE_MAPPER::toDto)
                .collect(Collectors.toList());
    }

    public SpexareDto create(final SpexareCreateDto dto) {
        return SPEXARE_MAPPER.toDto(repository.save(SPEXARE_MAPPER.toModel(dto)));
    }

    public Optional<SpexareDto> update(final SpexareUpdateDto dto) {
        return partialUpdate(dto);
    }

    public Optional<SpexareDto> partialUpdate(final SpexareUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(model -> {
                    SPEXARE_MAPPER.toPartialModel(dto, model);
                    return model;
                })
                .map(repository::save)
                .map(SPEXARE_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    public Optional<SpexareDto> saveImage(final Long id, final byte[] image, final String contentType) {
        return repository
                .findById(id)
                .map(model -> {
                    model.setImage(image);
                    model.setImageContentType(hasText(contentType) ? contentType : FileUtil.detectMimeType(image));
                    repository.save(model);
                    return SPEXARE_MAPPER.toDto(model);
                });
    }

    public Optional<SpexareDto> deleteImage(final Long id) {
        return repository
                .findById(id)
                .map(model -> {
                    model.setImage(null);
                    model.setImageContentType(null);
                    repository.save(model);
                    return SPEXARE_MAPPER.toDto(model);
                });
    }

    public Optional<Pair<byte[], String>> getImage(final Long id) {
        return repository
                .findById(id)
                .filter(model -> model.getImage() != null && hasText(model.getImageContentType()))
                .map(model -> Pair.of(model.getImage(), model.getImageContentType()));
    }

    public Optional<SpexareDto> findPartnerBySpexare(final Long spexareId) {
        return repository
                .findById(spexareId)
                .filter(spexare -> spexare.getPartner() != null)
                .map(Spexare::getPartner)
                .map(SPEXARE_MAPPER::toDto);
    }

    public Optional<SpexareDto> updatePartner(final Long spexareId, final Long id) {
        if (doSpexareAndPartnerExist(spexareId, id)) {
            repository
                    .findById(spexareId)
                    .ifPresent(spexare ->
                            repository
                                    .findById(id)
                                    .ifPresent(partner -> {
                                        spexare.setPartner(partner);
                                        partner.setPartner(spexare);
                                        repository.save(spexare);
                                        repository.save(partner);
                                    }));
            return findById(spexareId);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or partner %s do not exist", spexareId, id));
        }
    }

    public boolean deletePartner(final Long spexareId) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findById(spexareId)
                    .filter(spexare -> spexare.getPartner() != null)
                    .map(spexare -> {
                        final Spexare partner = spexare.getPartner();
                        spexare.setPartner(null);
                        partner.setPartner(null);
                        repository.save(spexare);
                        repository.save(partner);
                        return true;
                    })
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doSpexareAndPartnerExist(final Long spexareId, final Long partnerId) {
        return doesSpexareExist(spexareId) && doesSpexareExist(partnerId);
    }

}
