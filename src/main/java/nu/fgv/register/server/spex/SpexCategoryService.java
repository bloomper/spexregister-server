package nu.fgv.register.server.spex;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SpexCategoryService {

    private final SpexCategoryRepository repository;

    public SpexCategoryService(final SpexCategoryRepository repository) {
        this.repository = repository;
    }

    public List<SpexCategory> findAll() {
        return repository.findAll();
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
