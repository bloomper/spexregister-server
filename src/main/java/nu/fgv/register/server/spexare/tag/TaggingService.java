package nu.fgv.register.server.spexare.tag;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.tag.TagDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class TaggingService {

    private final TaggingRepository repository;

    private final SpexareRepository spexareRepository;

    public Page<TagDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findBySpexareId(spexareId, pageable)
                    .map(TAG_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public boolean create(final Long spexareId, final Long id) {
        if (doSpexareAndTagExist(spexareId, id)) {
            return repository
                    .findById(id)
                    .map(tag -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> !repository.existsBySpexareIdAndTagId(spexare.getId(), tag.getId()))
                            .map(spexare -> {
                                spexare.getTags().add(tag);
                                spexareRepository.save(spexare);
                                return true;
                            })
                            .orElse(false)
                    )
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or tag %s do not exist", spexareId, id));
        }
    }

    public boolean deleteById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findById(id)
                    .map(tag -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.existsBySpexareIdAndTagId(spexare.getId(), tag.getId()))
                            .map(spexare -> {
                                spexare.getTags().remove(tag);
                                spexareRepository.save(spexare);
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doSpexareAndTagExist(final Long spexareId, final Long tagId) {
        return doesSpexareExist(spexareId) && repository.existsById(tagId);
    }

}
