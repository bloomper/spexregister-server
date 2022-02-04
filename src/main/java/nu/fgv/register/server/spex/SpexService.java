package nu.fgv.register.server.spex;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SpexService {

    private final SpexRepository repository;

    public SpexService(final SpexRepository repository) {
        this.repository = repository;
    }

    public List<Spex> findAll() {
        return repository.findAll();
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
