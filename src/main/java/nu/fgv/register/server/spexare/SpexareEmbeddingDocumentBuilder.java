/*
 * Copyright 2024 the original author or authors.
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.CountryService;
import nu.fgv.register.server.settings.Type;
import nu.fgv.register.server.settings.TypeMapper;
import nu.fgv.register.server.spex.Spex;
import nu.fgv.register.server.spex.SpexDetails;
import nu.fgv.register.server.spexare.activity.Activity;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.actor.Actor;
import nu.fgv.register.server.spexare.address.Address;
import nu.fgv.register.server.spexare.consent.Consent;
import nu.fgv.register.server.spexare.membership.Membership;
import nu.fgv.register.server.spexare.toggle.Toggle;
import nu.fgv.register.server.tag.Tag;
import nu.fgv.register.server.task.Task;
import nu.fgv.register.server.task.category.TaskCategory;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SpexareEmbeddingDocumentBuilder {

    private final CountryService countryService;

    public String build(final Spexare spexare) {
        final String addresses = spexare.getAddresses() == null ? "" :
                spexare.getAddresses().stream()
                        .sorted(Comparator.comparing(a -> safe(typeLabel(a.getType()))))
                        .map(this::formatAddress)
                        .collect(Collectors.joining("\n"));

        final String consents = spexare.getConsents() == null ? "" :
                spexare.getConsents().stream()
                        .sorted(Comparator.comparing(c -> safe(typeLabel(c.getType()))))
                        .map(this::formatConsent)
                        .collect(Collectors.joining("\n"));

        final String memberships = spexare.getMemberships() == null ? "" :
                spexare.getMemberships().stream()
                        .sorted(Comparator.comparing(m -> safe(typeLabel(m.getType()))))
                        .map(this::formatMembership)
                        .collect(Collectors.joining("\n"));

        final String tags = spexare.getTags() == null ? "" :
                spexare.getTags().stream()
                        .map(Tag::getName)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .distinct()
                        .collect(Collectors.joining(", "));

        final String toggles = spexare.getToggles() == null ? "" :
                spexare.getToggles().stream()
                        .sorted(Comparator.comparing(t -> safe(typeLabel(t.getType()))))
                        .map(this::formatToggle)
                        .collect(Collectors.joining("\n"));

        final String activities = spexare.getActivities() == null ? "" :
                spexare.getActivities().stream()
                        .sorted(Comparator.comparing(this::activitySortKey).reversed())
                        .map(this::formatActivity)
                        .collect(Collectors.joining("\n"));

        final String ssnBirthDate = birthDateFromSocialSecurityNumber(spexare.getSocialSecurityNumber());
        final String deceased = formatBoolean(spexare.getDeceased());
        final String partner = formatPartner(spexare.getPartner());

        return """
                Typ: Spexare
                Namn: %s %s
                Smeknamn: %s
                Födelsedatum: %s
                Avliden: %s
                Partner: %s
                Examen: %s
                
                Adresser:
                %s
                
                Medgivanden:
                %s
                
                Medlemskap:
                %s
                
                Taggar: %s
                
                Inställningar:
                %s
                
                Aktiviteter:
                %s
                """.formatted(
                safe(spexare.getFirstName()),
                safe(spexare.getLastName()),
                safe(spexare.getNickName()),
                safe(ssnBirthDate),
                safe(deceased),
                safe(partner),
                safe(spexare.getGraduation()),
                safeBlock(addresses),
                safeBlock(consents),
                safeBlock(memberships),
                safe(tags),
                safeBlock(toggles),
                safeBlock(activities)
        ).trim();
    }

    private String formatBoolean(final Boolean value) {
        return value ? "ja" : "nej";
    }

    private String formatPartner(@Nullable final Spexare partner) {
        if (partner == null) {
            return "";
        }
        return "%s %s".formatted(safe(partner.getFirstName()), safe(partner.getLastName())).trim();
    }

    private String birthDateFromSocialSecurityNumber(@Nullable final String socialSecurityNumber) {
        if (socialSecurityNumber == null) {
            return "";
        }
        final String s = socialSecurityNumber.trim();
        if (s.isBlank()) {
            return "";
        }
        final int hyphenIdx = s.indexOf('-');

        return hyphenIdx >= 0 ? s.substring(0, hyphenIdx).trim() : s;
    }

    private String formatAddress(final Address address) {
        return """
                - Typ: %s
                  Adress: %s
                  Postnummer: %s
                  Stad: %s
                  Land: %s
                  Telefon: %s
                  Mobil: %s
                  E-post: %s
                """.formatted(
                safe(typeLabel(address.getType())),
                safe(address.getStreetAddress()),
                safe(address.getPostalCode()),
                safe(address.getCity()),
                safe(hasText(address.getCountry()) ? countryService.findByIsoCode(address.getCountry()).getLabel() : ""),
                safe(address.getPhone()),
                safe(address.getPhoneMobile()),
                safe(address.getEmailAddress())
        ).trim();
    }

    private String formatConsent(final Consent consent) {
        return """
                - Typ: %s
                  Värde: %s
                """.formatted(
                safe(typeLabel(consent.getType())),
                safe(formatBoolean(consent.getValue()))
        ).trim();
    }

    private String formatMembership(final Membership membership) {
        return """
                - Typ: %s
                  Kommentar: %s
                """.formatted(
                safe(typeLabel(membership.getType())),
                safe(membership.getYear())
        ).trim();
    }

    private String formatToggle(final Toggle toggle) {
        return """
                - Typ: %s
                  Värde: %s
                """.formatted(
                safe(typeLabel(toggle.getType())),
                safe(formatBoolean(toggle.getValue()))
        ).trim();
    }

    private String formatActivity(final Activity activity) {
        final SpexActivity spexActivity = activity.getSpexActivity();
        final Spex spex = spexActivity == null ? null : spexActivity.getSpex();
        final SpexDetails details = spex == null ? null : spex.getDetails();

        final String spexTitle = details == null ? "" : safe(details.getTitle());
        final String spexYear = spex == null ? "" : safe(spex.getYear());
        final String spexCategory = details == null || details.getCategory() == null ? "" : safe(details.getCategory().getName());
        final String revival = spex != null && spex.isRevival() ? " (nyuppsättning)" : "";

        final String tasks = activity.getTaskActivities() == null ? "" :
                activity.getTaskActivities().stream()
                        .map(TaskActivity::getTask)
                        .map(Task::getName)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .distinct()
                        .collect(Collectors.joining(", "));

        final String taskCategories = activity.getTaskActivities() == null ? "" :
                activity.getTaskActivities().stream()
                        .map(TaskActivity::getTask)
                        .map(Task::getCategory)
                        .filter(Objects::nonNull)
                        .map(TaskCategory::getName)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .distinct()
                        .collect(Collectors.joining(", "));

        final String roles = activity.getTaskActivities() == null ? "" :
                activity.getTaskActivities().stream()
                        .flatMap(ta -> ta.getActors() == null ? java.util.stream.Stream.empty() : ta.getActors().stream())
                        .map(Actor::getRole)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .distinct()
                        .collect(Collectors.joining(", "));

        final String vocals = activity.getTaskActivities() == null ? "" :
                activity.getTaskActivities().stream()
                        .flatMap(ta -> ta.getActors() == null ? java.util.stream.Stream.empty() : ta.getActors().stream())
                        .map(Actor::getVocal)
                        .map(this::typeLabel)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .distinct()
                        .collect(Collectors.joining(", "));

        return """
                - Spex: %s (%s)%s
                  Spexkategori: %s
                  Funktionskategorier: %s
                  Funktioner: %s
                  Roller: %s
                  Stämmor: %s
                """.formatted(
                spexTitle,
                spexYear,
                revival,
                safe(spexCategory),
                safe(taskCategories),
                safe(tasks),
                safe(roles),
                safe(vocals)
        ).trim();
    }

    private String activitySortKey(final Activity activity) {
        final SpexActivity spexActivity = activity.getSpexActivity();
        final Spex spex = spexActivity == null ? null : spexActivity.getSpex();

        return spex == null ? "" : safe(spex.getYear());
    }

    private String typeLabel(@Nullable final Type type) {
        if (type == null) {
            return "";
        }
        try {
            return safe(TypeMapper.TYPE_MAPPER.toDto(type).getLabel());
        } catch (final Exception e) {
            return "";
        }
    }

    private String safeBlock(@Nullable String s) {
        if (s == null) {
            return "";
        }
        s = s.strip();

        return s.isBlank() ? "" : s;
    }

    private String safe(@Nullable final String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

}
