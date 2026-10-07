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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Component
public class UserApprovalNotifier {

    private static final String SUBJECT = "Ditt konto för Spexregistret är godkänt";
    private static final String TEXT = """
            Hej!

            Ditt konto för Spexregistret har godkänts och du kan nu logga in.
            """;
    private static final String LOGIN_LINK = """

            Logga in här: %s
            """;

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String frontendUrl;

    public UserApprovalNotifier(final ObjectProvider<JavaMailSender> mailSender,
                                @Value("${spexregister.mail.from:}") final String from,
                                @Value("${spexregister.frontend-url:}") final String frontendUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    public boolean notifyApproved(final String email) {
        final JavaMailSender sender = mailSender.getIfAvailable();

        if (sender == null) {
            log.warn("No mail server configured, could not notify {} about approval", email);
            return false;
        }

        final SimpleMailMessage message = new SimpleMailMessage();

        if (hasText(from)) {
            message.setFrom(from);
        }
        message.setTo(email);
        message.setSubject(SUBJECT);
        message.setText(hasText(frontendUrl) ? TEXT + LOGIN_LINK.formatted(frontendUrl) : TEXT);

        try {
            sender.send(message);
            return true;
        } catch (final MailException e) {
            log.error("Could not notify {} about approval", email, e);
            return false;
        }
    }
}
