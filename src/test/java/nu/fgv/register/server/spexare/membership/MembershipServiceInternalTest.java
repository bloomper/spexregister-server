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

package nu.fgv.register.server.spexare.membership;

import nu.fgv.register.server.security.policy.PermissionService;
import nu.fgv.register.server.settings.TypeRepository;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.spexare.SpexareRepository;
import nu.fgv.register.server.sync.MedlemskapProperties;
import nu.fgv.register.server.util.error.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Internal memberships (FGV) are Medlemskap's and written only by the sync.
 */
class MembershipServiceInternalTest {

    private final MedlemskapProperties properties = new MedlemskapProperties(true, "", "", "", List.of("FGV"));
    private final MembershipService service = new MembershipService(mock(MembershipRepository.class), mock(SpexareRepository.class),
            mock(TypeRepository.class), mock(TypeService.class), mock(PermissionService.class), properties);

    @Test
    void creatingAnInternalMembershipIsRefused() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), "FGV", MembershipCreateDto.builder().year("2025").build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void theTypeIsComparedIgnoringCase() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), "fgv", MembershipCreateDto.builder().year("2025").build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deletingAnInternalMembershipIsRefused() {
        assertThatThrownBy(() -> service.deleteById(UUID.randomUUID(), "FGV", UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void onlyTheConfiguredTypesAreInternal() {
        assertThat(properties.isInternal("FGV")).isTrue();
        assertThat(properties.isInternal("CING")).isFalse();
    }
}
