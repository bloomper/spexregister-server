package nu.fgv.register.server.spex;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class SpexService {

    private final SpexRepository repository;

    public Page<Spex> find(final Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Optional<Spex> findById(final Long id) {
        return repository.findById(id);
    }

    public Spex save(final Spex model) {
        return repository.save(model);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    public List<Spex> findAllRevivals(final Long id) {
        return repository.findAllRevivals(id);
    }
}
