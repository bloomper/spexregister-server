package nu.fgv.register.server.spex;

import org.jeasy.random.EasyRandomExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(EasyRandomExtension.class)
public class SpexServiceTest {

    @InjectMocks
    private SpexService service;

    @Mock
    private SpexRepository repository;

    @Mock
    private SpexCategoryRepository categoryRepository;

    @Test
    public void givenNoModels_whenFind_thenReturnEmptyPagedDtos() {
        // given
        when(repository.findAllByParentIsNull(any(Pageable.class))).thenReturn(Page.empty());

        // when
        final Page<SpexDto> page = service.find(false, Pageable.unpaged());

        // then
        assertThat(page.getTotalElements(), is(0L));
    }
}
