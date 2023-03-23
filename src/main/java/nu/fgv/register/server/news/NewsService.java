package nu.fgv.register.server.news;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static nu.fgv.register.server.news.NewsMapper.NEWS_MAPPER;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class NewsService {

    private final NewsRepository repository;

    public List<NewsDto> findAll(final Sort sort) {
        return repository
                .findAll(sort)
                .stream()
                .map(NEWS_MAPPER::toDto)
                .collect(Collectors.toList());
    }

    public Page<NewsDto> find(final Pageable pageable) {
        return repository
                .findAll(pageable)
                .map(NEWS_MAPPER::toDto);
    }

    public Optional<NewsDto> findById(final Long id) {
        return repository
                .findById(id)
                .map(NEWS_MAPPER::toDto);
    }

    public NewsDto create(final NewsCreateDto dto) {
        return NEWS_MAPPER.toDto(repository.save(NEWS_MAPPER.toModel(dto)));
    }

    public Optional<NewsDto> update(final NewsUpdateDto dto) {
        return partialUpdate(dto);
    }

    public Optional<NewsDto> partialUpdate(final NewsUpdateDto dto) {
        return repository
                .findById(dto.getId())
                .map(news -> {
                    NEWS_MAPPER.toPartialModel(dto, news);
                    return news;
                })
                .map(repository::save)
                .map(NEWS_MAPPER::toDto);
    }

    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

}
