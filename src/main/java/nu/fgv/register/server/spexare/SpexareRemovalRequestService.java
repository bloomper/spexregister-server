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

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.util.error.BadRequestException;
import nu.fgv.register.server.util.error.InternalErrorException;
import nu.fgv.register.server.util.security.RequiresAdminOrEditorOrUser;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserEmailClaim;
import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserNameClaim;
import static nu.fgv.register.server.util.security.SecurityUtil.getCurrentUserSubClaim;
import static org.springframework.util.StringUtils.hasText;

/**
 * Lets a user ask to be removed from the register, by mail to the administrators named in
 * {@code spexregister.mail.admin-recipients}.
 *
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class SpexareRemovalRequestService {

    static final int REASON_MAX_LENGTH = 2000;

    private final SpexareService spexareService;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final MessageSource messageSource;
    private final String from;
    private final List<String> recipients;
    private final Locale locale;

    public SpexareRemovalRequestService(final SpexareService spexareService,
                                        final ObjectProvider<JavaMailSender> mailSender,
                                        final MessageSource messageSource,
                                        @Value("${spexregister.mail.from}") final String from,
                                        @Value("${spexregister.mail.admin-recipients}") final List<String> recipients,
                                        @Value("${spexregister.default-language}") final String language) {
        this.spexareService = spexareService;
        this.mailSender = mailSender;
        this.messageSource = messageSource;
        this.from = from;
        this.recipients = recipients.stream().filter(recipient -> hasText(recipient)).toList();
        this.locale = Locale.forLanguageTag(language);
    }

    /**
     * Mails the request with the user's name, email, Keycloak subject and spexare, replying to the user's email.
     *
     * @param reason why, at most {@value #REASON_MAX_LENGTH} characters, or null
     * @throws nu.fgv.register.server.util.error.InternalErrorException when mail is not configured or could not be sent
     */
    @RequiresAdminOrEditorOrUser
    public void request(final @Nullable String reason) {
        if (reason != null && reason.length() > REASON_MAX_LENGTH) {
            throw new BadRequestException("Reason must not be longer than %d characters".formatted(REASON_MAX_LENGTH));
        }

        final JavaMailSender sender = mailSender.getIfAvailable();

        if (sender == null || recipients.isEmpty()) {
            log.error("Could not send removal request, mail is not configured or there are no recipients");
            throw new InternalErrorException("Removal requests cannot be sent at the moment");
        }

        final Optional<SpexareDto> spexare = spexareService.findByCurrentUser();
        final String email = getCurrentUserEmailClaim();
        final String none = messageSource.getMessage("spexare.removalRequest.none", null, locale);
        final SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(recipients.toArray(String[]::new));
        if (hasText(email)) {
            message.setReplyTo(email);
        }
        message.setSubject(messageSource.getMessage("spexare.removalRequest.subject", null, locale));
        message.setText(messageSource.getMessage("spexare.removalRequest.text", new Object[]{
                orElse(getCurrentUserNameClaim(), none),
                orElse(email, none),
                getCurrentUserSubClaim(),
                spexare.map(dto -> "%s %s".formatted(dto.getFirstName(), dto.getLastName())).orElse(none),
                spexare.map(dto -> String.valueOf(dto.getId())).orElse(none),
                hasText(reason) ? reason.strip() : none
        }, locale));

        try {
            sender.send(message);
            log.info("Sent removal request for user {}", getCurrentUserSubClaim());
        } catch (final MailException e) {
            log.error("Could not send removal request for user {}", getCurrentUserSubClaim(), e);
            throw new InternalErrorException("Removal requests cannot be sent at the moment");
        }
    }

    private static String orElse(final String value, final String fallback) {
        return hasText(value) ? value : fallback;
    }
}
