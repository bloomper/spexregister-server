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
import java.util.UUID;

import static nu.fgv.register.server.util.TestUuids.uuid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
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

    private static UserImpexDto user(final UUID id, final ImpexAction action, final UUID spexareId) {
        final UserImpexDto dto = new UserImpexDto();

        dto.setId(id);
        dto.setEmail("anna@example.org");
        dto.setStateId("ACTIVE");
        dto.setSpexareId(spexareId);
        dto.setAction(action);
        dto.setRowNumber(2);
        return dto;
    }

    private static AuthorityImpexDto authority(final UUID userId, final ImpexAction action) {
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
        when(userService.create(any())).thenReturn(UserDto.builder().id(uuid(40)).email("anna@example.org").externalId("kc-1")
                .temporaryPassword("tillfälligt").build());

        final ImportResultDto result = service.processImport(Map.of(UserImpexDto.class, List.of(user(uuid(1001), ImpexAction.CREATE, uuid(7))),
                AuthorityImpexDto.class, List.of(authority(uuid(1001), ImpexAction.CREATE))), Locale.ENGLISH);

        assertThat(result.getErrors(), empty());
        verify(userService).setState(uuid(40), "ACTIVE");
        verify(userService).addSpexare(uuid(40), uuid(7));
        verify(userService).addAuthority(uuid(40), "EDITOR");
        assertThat(result.getData().get("users"), is(Map.of("anna@example.org", Map.of("externalId", "kc-1", "temporaryPassword", "tillfälligt"))));
    }

    @Test
    void should_change_or_remove_a_users_spexare_on_update() {
        when(userService.partialUpdate(any())).thenReturn(UserDto.builder().id(uuid(5)).build());
        when(userService.findSpexareByUser(uuid(5))).thenReturn(Optional.of(SpexareDto.builder().id(uuid(7)).build()));

        service.processImport(Map.of(UserImpexDto.class, List.of(user(uuid(5), ImpexAction.UPDATE, uuid(7)))), Locale.ENGLISH);
        verify(userService, never()).addSpexare(any(UUID.class), any(UUID.class));

        service.processImport(Map.of(UserImpexDto.class, List.of(user(uuid(5), ImpexAction.UPDATE, uuid(8)))), Locale.ENGLISH);
        verify(userService).addSpexare(uuid(5), uuid(8));

        final ImportResultDto result = service.processImport(Map.of(UserImpexDto.class, List.of(user(uuid(5), ImpexAction.UPDATE, null))), Locale.ENGLISH);
        verify(userService).removeSpexare(uuid(5));
        assertThat(result.getData().get("users"), is(nullValue()));
    }

    @Test
    void should_delete_users_and_remove_authorities() {
        service.processImport(Map.of(UserImpexDto.class, List.of(user(uuid(5), ImpexAction.DELETE, null)),
                AuthorityImpexDto.class, List.of(authority(uuid(6), ImpexAction.DELETE))), Locale.ENGLISH);

        verify(userService).deleteById(uuid(5));
        verify(userService).removeAuthority(uuid(6), "EDITOR");
    }
}
