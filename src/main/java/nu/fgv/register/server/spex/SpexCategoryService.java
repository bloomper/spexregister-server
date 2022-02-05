package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexCategoryService {

    private final SpexCategoryRepository repository;

    public Page<SpexCategory> find(final Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<SpexCategory> findById(final Long id) {
        return repository.findById(id);
    }

    public SpexCategory save(final SpexCategory model) {
        return repository.save(model);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }
}
