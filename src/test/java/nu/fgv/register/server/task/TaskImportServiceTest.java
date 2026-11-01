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
import java.util.UUID;

import static nu.fgv.register.server.util.TestUuids.uuid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
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

    private static TaskImpexDto update(final UUID categoryId) {
        final TaskImpexDto dto = new TaskImpexDto();

        dto.setId(uuid(1));
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

    @Test
    void should_set_the_category_when_updating_a_task_without_one() {
        when(taskService.partialUpdate(any())).thenReturn(TaskDto.builder().id(uuid(1)).build());
        when(taskService.findCategoryByTask(uuid(1))).thenThrow(new ResourceNoValueException(Task.class, "category", uuid(1)));

        final ImportResultDto result = service.processImport(Map.of(TaskImpexDto.class, List.of(update(uuid(2)))), Locale.ENGLISH);

        assertThat(result.getErrors(), empty());
        verify(taskService).addCategory(uuid(1), uuid(2));
    }

    @Test
    void should_keep_the_category_when_updating_a_task_already_in_it() {
        when(taskService.partialUpdate(any())).thenReturn(TaskDto.builder().id(uuid(1)).build());
        when(taskService.findCategoryByTask(uuid(1))).thenReturn(TaskCategoryDto.builder().id(uuid(2)).build());

        final ImportResultDto result = service.processImport(Map.of(TaskImpexDto.class, List.of(update(uuid(2)))), Locale.ENGLISH);

        assertThat(result.getErrors(), empty());
        verify(taskService, never()).addCategory(any(UUID.class), any(UUID.class));
    }
}
