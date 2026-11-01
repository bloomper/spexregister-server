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

package nu.fgv.register.server.user;

import nu.fgv.register.server.impex.model.ImpexAction;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.spexare.SpexareDto;
import nu.fgv.register.server.spexare.SpexareService;
import nu.fgv.register.server.user.authority.AuthorityImpexDto;
import nu.fgv.register.server.user.authority.AuthorityService;
import nu.fgv.register.server.user.state.StateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Importing users: their state, spexare and authorities, and the temporary passwords of new users.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
class UserImportServiceTest {

    private final UserService userService = mock(UserService.class);
    private UserImportService service;

    private static UserImpexDto user(final Long id, final ImpexAction action, final Long spexareId) {
        final UserImpexDto dto = new UserImpexDto();

        dto.setId(id);
        dto.setEmail("anna@example.org");
        dto.setStateId("ACTIVE");
        dto.setSpexareId(spexareId);
        dto.setAction(action);
        dto.setRowNumber(2);
        return dto;
    }

    private static AuthorityImpexDto authority(final Long userId, final ImpexAction action) {
        final AuthorityImpexDto dto = new AuthorityImpexDto();

        dto.setUserId(userId);
        dto.setId("EDITOR");
        dto.setAction(action);
        dto.setRowNumber(2);
        return dto;
    }

    @BeforeEach
    void setUp() {
        final StaticMessageSource messages = new StaticMessageSource();

        messages.setUseCodeAsDefaultMessage(true);
        service = new UserImportService(List.of(), userService, mock(StateService.class), mock(AuthorityService.class), mock(SpexareService.class),
                messages, mock(PlatformTransactionManager.class));
    }

    @Test
    void should_create_a_user_with_state_spexare_and_authority_and_return_the_temporary_password() {
        when(userService.create(any())).thenReturn(UserDto.builder().id(40L).email("anna@example.org").externalId("kc-1")
                .temporaryPassword("tillfälligt").build());

        final ImportResultDto result = service.processImport(Map.of(UserImpexDto.class, List.of(user(-1L, ImpexAction.CREATE, 7L)),
                AuthorityImpexDto.class, List.of(authority(-1L, ImpexAction.CREATE))), Locale.ENGLISH);

        assertThat(result.getErrors(), empty());
        verify(userService).setState(40L, "ACTIVE");
        verify(userService).addSpexare(40L, 7L);
        verify(userService).addAuthority(40L, "EDITOR");
        assertThat(result.getData().get("users"), is(Map.of("anna@example.org", Map.of("externalId", "kc-1", "temporaryPassword", "tillfälligt"))));
    }

    @Test
    void should_change_or_remove_a_users_spexare_on_update() {
        when(userService.partialUpdate(any())).thenReturn(UserDto.builder().id(5L).build());
        when(userService.findSpexareByUser(5L)).thenReturn(Optional.of(SpexareDto.builder().id(7L).build()));

        service.processImport(Map.of(UserImpexDto.class, List.of(user(5L, ImpexAction.UPDATE, 7L))), Locale.ENGLISH);
        verify(userService, never()).addSpexare(anyLong(), anyLong());

        service.processImport(Map.of(UserImpexDto.class, List.of(user(5L, ImpexAction.UPDATE, 8L))), Locale.ENGLISH);
        verify(userService).addSpexare(5L, 8L);

        final ImportResultDto result = service.processImport(Map.of(UserImpexDto.class, List.of(user(5L, ImpexAction.UPDATE, null))), Locale.ENGLISH);
        verify(userService).removeSpexare(5L);
        assertThat(result.getData().get("users"), is(nullValue()));
    }

    @Test
    void should_delete_users_and_remove_authorities() {
        service.processImport(Map.of(UserImpexDto.class, List.of(user(5L, ImpexAction.DELETE, null)),
                AuthorityImpexDto.class, List.of(authority(6L, ImpexAction.DELETE))), Locale.ENGLISH);

        verify(userService).deleteById(5L);
        verify(userService).removeAuthority(6L, "EDITOR");
    }
}
