/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nu.fgv.register.server.task;

import nu.fgv.register.server.impex.model.ImpexAction;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.task.category.TaskCategoryDto;
import nu.fgv.register.server.util.error.ResourceNoValueException;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class TaskImportServiceTest {

    private final TaskService taskService = mock(TaskService.class);
    private final TaskImportService service = new TaskImportService(
            List.of(),
            taskService,
            messageSource(),
            mock(PlatformTransactionManager.class)
    );

    @Test
    void should_set_the_category_when_updating_a_task_without_one() {
        when(taskService.partialUpdate(any())).thenReturn(TaskDto.builder().id(1L).build());
        when(taskService.findCategoryByTask(1L)).thenThrow(new ResourceNoValueException(Task.class, "category", 1L));

        final ImportResultDto result = service.processImport(Map.of(TaskImpexDto.class, List.of(update(2L))), Locale.ENGLISH);

        assertThat(result.getErrors(), empty());
        verify(taskService).addCategory(1L, 2L);
    }

    @Test
    void should_keep_the_category_when_updating_a_task_already_in_it() {
        when(taskService.partialUpdate(any())).thenReturn(TaskDto.builder().id(1L).build());
        when(taskService.findCategoryByTask(1L)).thenReturn(TaskCategoryDto.builder().id(2L).build());

        final ImportResultDto result = service.processImport(Map.of(TaskImpexDto.class, List.of(update(2L))), Locale.ENGLISH);

        assertThat(result.getErrors(), empty());
        verify(taskService, never()).addCategory(anyLong(), anyLong());
    }

    private static TaskImpexDto update(final Long categoryId) {
        final TaskImpexDto dto = new TaskImpexDto();

        dto.setId(1L);
        dto.setName("Name");
        dto.setCategoryId(categoryId);
        dto.setAction(ImpexAction.UPDATE);
        dto.setRowNumber(2);
        return dto;
    }

    private static StaticMessageSource messageSource() {
        final StaticMessageSource messageSource = new StaticMessageSource();

        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }
}
