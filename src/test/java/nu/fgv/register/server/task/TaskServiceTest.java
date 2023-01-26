package nu.fgv.register.server.task;

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
public class TaskServiceTest {

    @InjectMocks
    private TaskService service;

    @Mock
    private TaskRepository repository;

    @Random
    private Task task1;

    @Random
    private Task task2;

    @Test
    public void givenNoModels_whenFind_thenReturnEmptyPagedDtos() {
        // given
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        // when
        final Page<TaskDto> page = service.find(Pageable.unpaged());

        // then
        assertThat(page.getTotalElements(), is(0L));
    }

    @Test
    public void givenModels_whenFind_thenReturnPagedDtos() {
        // given
        when(repository.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(task1, task2)));

        // when
        final Page<TaskDto> page = service.find(Pageable.unpaged());

        // then
        assertThat(page.getTotalElements(), is(2L));
    }

    @Test
    public void givenNoModel_whenFindById_thenReturnDto() {
        // given
        when(repository.findById(any(Long.class))).thenReturn(Optional.empty());

        // when
        final Optional<TaskDto> dto = service.findById(1L);

        // then
        assertThat(dto.isPresent(), is(false));
    }

    @Test
    public void givenModel_whenFindById_thenReturnDto() {
        // given
        when(repository.findById(any(Long.class))).thenReturn(Optional.of(task1));

        // when
        final Optional<TaskDto> dto = service.findById(1L);

        // then
        assertThat(dto.isPresent(), is(true));
    }

}
