package nu.fgv.register.server.spexare.address;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeType;
import nu.fgv.register.server.spexare.SpexareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static nu.fgv.register.server.spexare.address.AddressMapper.ADDRESS_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class AddressService {

    private final AddressRepository repository;

    private final SpexareRepository spexareRepository;
    private final TypeRepository typeRepository;

    public Page<AddressDto> findBySpexare(final Long spexareId, final Pageable pageable) {
        if (doesSpexareExist(spexareId)) {
            return spexareRepository
                    .findById(spexareId)
                    .map(spexare -> repository
                            .findBySpexare(spexare, pageable)
                            .map(ADDRESS_MAPPER::toDto)
                    )
                    .orElseGet(Page::empty);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<AddressDto> findById(final Long spexareId, final Long id) {
        if (doesSpexareExist(spexareId)) {
            return repository
                    .findById(id)
                    .filter(address -> address.getSpexare().getId().equals(spexareId))
                    .map(ADDRESS_MAPPER::toDto);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s does not exist", spexareId));
        }
    }

    public Optional<AddressDto> create(final Long spexareId, final String typeId, final AddressCreateDto dto) {
        if (doSpexareAndTypeExist(spexareId, typeId)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> !repository.existsBySpexareAndType(spexare, type))
                            .map(spexare -> {
                                final Address address = ADDRESS_MAPPER.toModel(dto);
                                address.setSpexare(spexare);
                                address.setType(type);
                                return repository.save(address);
                            })
                            .map(ADDRESS_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s and/or type %s do not exist", spexareId, typeId));
        }
    }

    public Optional<AddressDto> update(final Long spexareId, final String typeId, final Long id, final AddressUpdateDto dto) {
        return partialUpdate(spexareId, typeId, id, dto);
    }

    public Optional<AddressDto> partialUpdate(final Long spexareId, final String typeId, final Long id, final AddressUpdateDto dto) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesAddressExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .flatMap(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.existsBySpexareAndTypeAndId(spexare, type, id))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(address -> address.getSpexare().getId().equals(spexareId))
                            .map(address -> {
                                ADDRESS_MAPPER.toPartialModel(dto, address);
                                return address;
                            })
                            .map(repository::save)
                            .map(ADDRESS_MAPPER::toDto)
                    );
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, type %s and/or address %s do not exist", spexareId, typeId, id));
        }
    }

    public boolean deleteById(final Long spexareId, final String typeId, final Long id) {
        if (doSpexareAndTypeExist(spexareId, typeId) && doesAddressExist(id)) {
            return typeRepository
                    .findById(typeId)
                    .map(type -> spexareRepository
                            .findById(spexareId)
                            .filter(spexare -> repository.existsBySpexareAndTypeAndId(spexare, type, id))
                            .flatMap(spexare -> repository.findById(id))
                            .filter(address -> address.getSpexare().getId().equals(spexareId))
                            .map(address -> {
                                repository.deleteById(address.getId());
                                return true;
                            })
                            .orElse(false))
                    .orElse(false);
        } else {
            throw new ResourceNotFoundException(String.format("Spexare %s, type %s and/or address %s do not exist", spexareId, typeId, id));
        }
    }

    private boolean doesSpexareExist(final Long id) {
        return spexareRepository.existsById(id);
    }

    private boolean doesAddressExist(final Long id) {
        return repository.existsById(id);
    }

    private boolean doSpexareAndTypeExist(final Long spexareId, final String typeId) {
        return doesSpexareExist(spexareId) && typeRepository.existsByIdAndType(typeId, TypeType.ADDRESS);
    }

}
