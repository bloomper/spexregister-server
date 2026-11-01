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

package nu.fgv.register.server.spex;

import nu.fgv.register.server.impex.model.ImpexAction;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.spex.category.SpexCategoryDto;
import nu.fgv.register.server.spex.category.SpexCategoryService;
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
class SpexImportServiceTest {

    private final SpexService spexService = mock(SpexService.class);
    private final SpexImportService service = new SpexImportService(
            List.of(),
            spexService,
            mock(SpexCategoryService.class),
            messageSource(),
            mock(PlatformTransactionManager.class),
            "https://register.example.com"
    );

    private static SpexImpexDto update(final UUID categoryId) {
        final SpexImpexDto dto = new SpexImpexDto();

        dto.setId(uuid(1));
        dto.setYear("2020");
        dto.setTitle("Title");
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
    void should_set_the_category_when_updating_a_spex_without_one() {
        when(spexService.partialUpdate(any())).thenReturn(SpexDto.builder().id(uuid(1)).build());
        when(spexService.findCategoriesBySpex(List.of(uuid(1)))).thenReturn(Map.of());

        final ImportResultDto result = service.processImport(Map.of(SpexImpexDto.class, List.of(update(uuid(2)))), Locale.ENGLISH);

        assertThat(result.getErrors(), empty());
        verify(spexService).addCategory(uuid(1), uuid(2));
    }

    @Test
    void should_keep_the_category_when_updating_a_spex_already_in_it() {
        when(spexService.partialUpdate(any())).thenReturn(SpexDto.builder().id(uuid(1)).build());
        when(spexService.findCategoriesBySpex(List.of(uuid(1)))).thenReturn(Map.of(uuid(1), SpexCategoryDto.builder().id(uuid(2)).build()));

        final ImportResultDto result = service.processImport(Map.of(SpexImpexDto.class, List.of(update(uuid(2)))), Locale.ENGLISH);

        assertThat(result.getErrors(), empty());
        verify(spexService, never()).addCategory(any(UUID.class), any(UUID.class));
    }
}
