package nu.fgv.register.server.spex;

import org.jeasy.random.EasyRandomExtension;
import org.jeasy.random.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(EasyRandomExtension.class)
public class SpexCategoryServiceTest {

    @InjectMocks
    private SpexCategoryService service;

    @Mock
    private SpexCategoryRepository repository;

    @Random
    private SpexCategory category1;

    @Random
    private SpexCategory category2;

    @Test
    public void givenNoModels_whenFind_thenReturnEmptyPagedDtos() {
        // given
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        // when
        final Page<SpexCategoryDto> page = service.find(Pageable.unpaged());

        // then
        assertThat(page.getTotalElements(), is(0L));
    }

    @Test
    public void givenModels_whenFind_thenReturnPagedDtos() {
        // given
        when(repository.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(category1, category2)));

        // when
        final Page<SpexCategoryDto> page = service.find(Pageable.unpaged());

        // then
        assertThat(page.getTotalElements(), is(2L));
        assertThat(page.getContent().get(0).getFirstYear(), is(category1.getFirstYear()));
        assertThat(page.getContent().get(1).getFirstYear(), is(category2.getFirstYear()));
    }

    @Test
    public void givenNoModel_whenFindById_thenReturnDto() {
        // given
        when(repository.findById(any(Long.class))).thenReturn(Optional.empty());

        // when
        final Optional<SpexCategoryDto> dto = service.findById(1L);

        // then
        assertThat(dto.isPresent(), is(false));
    }

    @Test
    public void givenModel_whenFindById_thenReturnDto() {
        // given
        when(repository.findById(any(Long.class))).thenReturn(Optional.of(category1));

        // when
        final Optional<SpexCategoryDto> dto = service.findById(1L);

        // then
        assertThat(dto.isPresent(), is(true));
        assertThat(dto.get().getFirstYear(), is(category1.getFirstYear()));
    }

    @Test
    public void givenModel_whenSave_thenReturnDto() {
        // given
        when(repository.save(any(SpexCategory.class))).thenReturn(category1);

        // when
        final SpexCategoryDto dto = SpexCategoryDto.builder().firstYear("1948").name("category").build();
        final SpexCategoryDto newDto = service.save(dto);

        // then
        assertThat(newDto.getId(), is(category1.getId()));
        assertThat(newDto.getFirstYear(), is(category1.getFirstYear()));
    }
}
