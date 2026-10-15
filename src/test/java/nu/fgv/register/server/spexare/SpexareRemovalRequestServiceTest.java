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

import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.InternalErrorException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
class SpexareRemovalRequestServiceTest {

    private final SpexareService spexareService = mock(SpexareService.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);
    private final ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

    @BeforeEach
    void setUp() {
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");

        final Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-id")
                .claim("email", "jane.doe@example.org")
                .claim("name", "Jane Doe")
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SpexareRemovalRequestService service(final List<String> recipients) {
        return new SpexareRemovalRequestService(spexareService, mailSenderProvider, messageSource, "noreply@example.org", recipients, "en");
    }

    @Test
    void should_send_request_to_recipients() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(spexareService.findByCurrentUser()).thenReturn(Optional.of(SpexareDto.builder().id(4711L).firstName("Jane").lastName("Doe").build()));

        service(List.of("admin@example.org", "other@example.org")).request("Please remove me");

        final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        final SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo(), arrayContaining("admin@example.org", "other@example.org"));
        assertThat(message.getFrom(), is("noreply@example.org"));
        assertThat(message.getReplyTo(), is("jane.doe@example.org"));
        assertThat(message.getSubject(), is("Request for removal from Spexregister"));
        assertThat(message.getText(), containsString("Name: Jane Doe"));
        assertThat(message.getText(), containsString("User ID: user-id"));
        assertThat(message.getText(), containsString("Spexare ID: 4711"));
        assertThat(message.getText(), containsString("Please remove me"));
    }

    @Test
    void should_send_request_without_spexare() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(spexareService.findByCurrentUser()).thenReturn(Optional.empty());

        service(List.of("admin@example.org")).request(null);

        final ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText(), containsString("Spexare ID: -"));
    }

    @Test
    void should_fail_when_mail_is_not_configured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        assertThrows(InternalErrorException.class, () -> service(List.of("admin@example.org")).request(null));
    }

    @Test
    void should_fail_when_there_are_no_recipients() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        assertThrows(InternalErrorException.class, () -> service(List.of("")).request(null));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_fail_when_sending_fails() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(spexareService.findByCurrentUser()).thenReturn(Optional.empty());
        doThrow(new MailSendException("down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(InternalErrorException.class, () -> service(List.of("admin@example.org")).request(null));
    }

    @Test
    void should_reject_too_long_reason() {
        assertThrows(BadRequestException.class, () -> service(List.of("admin@example.org")).request("x".repeat(SpexareRemovalRequestService.REASON_MAX_LENGTH + 1)));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
