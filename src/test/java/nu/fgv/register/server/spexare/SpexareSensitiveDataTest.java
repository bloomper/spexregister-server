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

package nu.fgv.register.server.spexare;

import nu.fgv.register.server.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_EDITOR;
import static nu.fgv.register.server.util.security.SecurityUtil.ROLE_USER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareSensitiveDataTest {

    private static final String SOCIAL_SECURITY_NUMBER = "19850101-1234";
    private static final String COMMENT = "Bass in the 1985 spex";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_show_only_the_birth_date_to_a_user() {
        authenticate("someone-else", ROLE_USER);

        final SpexareDto dto = SPEXARE_MAPPER.toDto(spexare("owner"));

        assertThat(dto.getSocialSecurityNumber(), is(equalTo("19850101")));
        assertThat(dto.getComment(), is(equalTo(COMMENT)));
        assertThat(dto.getFirstName(), is(equalTo("Ada")));
    }

    @Test
    void should_show_sensitive_fields_to_the_person_themselves() {
        authenticate("owner", ROLE_USER);

        final SpexareDto dto = SPEXARE_MAPPER.toDto(spexare("owner"));

        assertThat(dto.getSocialSecurityNumber(), is(equalTo(SOCIAL_SECURITY_NUMBER)));
        assertThat(dto.getComment(), is(equalTo(COMMENT)));
    }

    @Test
    void should_show_sensitive_fields_to_an_editor() {
        authenticate("editor", ROLE_EDITOR);

        final SpexareDto dto = SPEXARE_MAPPER.toDto(spexare(null));

        assertThat(dto.getSocialSecurityNumber(), is(equalTo(SOCIAL_SECURITY_NUMBER)));
        assertThat(dto.getComment(), is(equalTo(COMMENT)));
    }

    @Test
    void should_hide_sensitive_fields_without_authentication() {
        final SpexareDto dto = SPEXARE_MAPPER.toDto(spexare("owner"));

        assertThat(dto.getSocialSecurityNumber(), is(equalTo("19850101")));
        assertThat(dto.getComment(), is(equalTo(COMMENT)));
    }

    @Test
    void should_reduce_a_social_security_number_to_its_birth_date() {
        assertThat(SpexareSensitiveData.birthDateOf("19850101-1234"), is(equalTo("19850101")));
        assertThat(SpexareSensitiveData.birthDateOf("19850101"), is(equalTo("19850101")));
        assertThat(SpexareSensitiveData.birthDateOf(null), is(nullValue()));
    }

    @Test
    void should_index_the_birth_date_part() {
        final SocialSecurityNumberBirthDateBridge bridge = new SocialSecurityNumberBirthDateBridge();

        assertThat(bridge.toIndexedValue("19850101-1234", null), is(equalTo("19850101")));
        assertThat(bridge.toIndexedValue("1985-01-01", null), is(equalTo("19850101")));
        assertThat(bridge.toIndexedValue(null, null), is(nullValue()));
    }

    @Test
    void should_recognise_sensitive_paths() {
        assertThat(SpexareSensitiveData.isSensitivePath("comment"), is(false));
        assertThat(SpexareSensitiveData.isSensitivePath("partner.socialSecurityNumber"), is(true));
        assertThat(SpexareSensitiveData.isSensitivePath("firstName"), is(false));
    }

    private static Spexare spexare(final String externalId) {
        final Spexare spexare = new Spexare();

        spexare.setId(1L);
        spexare.setFirstName("Ada");
        spexare.setLastName("Lovelace");
        spexare.setSocialSecurityNumber(SOCIAL_SECURITY_NUMBER);
        spexare.setComment(COMMENT);
        if (externalId != null) {
            final User user = new User();

            user.setExternalId(externalId);
            spexare.setUser(user);
        }
        return spexare;
    }

    private static void authenticate(final String subject, final String role) {
        final Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(subject).build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role))));
    }
}
