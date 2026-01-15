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

import lombok.extern.slf4j.Slf4j;
import nu.fgv.register.server.settings.CountryDto;
import nu.fgv.register.server.settings.CountryService;
import nu.fgv.register.server.settings.TypeImpexDto;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.spex.SpexImpexDto;
import nu.fgv.register.server.spex.SpexRevivalImpexDto;
import nu.fgv.register.server.spex.SpexService;
import nu.fgv.register.server.spexare.activity.ActivityImpexDto;
import nu.fgv.register.server.spexare.activity.spex.SpexActivity;
import nu.fgv.register.server.spexare.activity.task.TaskActivity;
import nu.fgv.register.server.spexare.activity.task.actor.Actor;
import nu.fgv.register.server.spexare.address.AddressImpexDto;
import nu.fgv.register.server.spexare.consent.ConsentImpexDto;
import nu.fgv.register.server.spexare.membership.MembershipImpexDto;
import nu.fgv.register.server.spexare.tagging.TaggingImpexDto;
import nu.fgv.register.server.spexare.toggle.ToggleImpexDto;
import nu.fgv.register.server.tag.TagImpexDto;
import nu.fgv.register.server.tag.TagService;
import nu.fgv.register.server.task.TaskImpexDto;
import nu.fgv.register.server.task.TaskService;
import nu.fgv.register.server.util.impex.exporting.AbstractExportService;
import nu.fgv.register.server.util.impex.exporting.ExportEngine;
import nu.fgv.register.server.util.impex.model.ReportHolder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static nu.fgv.register.server.settings.TypeMapper.TYPE_MAPPER;
import static nu.fgv.register.server.spex.SpexMapper.SPEX_MAPPER;
import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.spexare.activity.ActivityMapper.ACTIVITY_MAPPER;
import static nu.fgv.register.server.spexare.address.AddressMapper.ADDRESS_MAPPER;
import static nu.fgv.register.server.spexare.consent.ConsentMapper.CONSENT_MAPPER;
import static nu.fgv.register.server.spexare.membership.MembershipMapper.MEMBERSHIP_MAPPER;
import static nu.fgv.register.server.spexare.tagging.TaggingMapper.TAGGING_MAPPER;
import static nu.fgv.register.server.spexare.toggle.ToggleMapper.TOGGLE_MAPPER;
import static nu.fgv.register.server.tag.TagMapper.TAG_MAPPER;
import static nu.fgv.register.server.task.TaskMapper.TASK_MAPPER;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class SpexareExportService extends AbstractExportService {

    private final SpexareService service;
    private final CountryService countryService;
    private final SpexService spexService;
    private final TaskService taskService;
    private final TagService tagService;
    private final TypeService typeService;

    public SpexareExportService(final SpexareService service,
                                final CountryService countryService,
                                final SpexService spexService,
                                final TaskService taskService,
                                final TagService tagService,
                                final TypeService typeService,
                                final List<ExportEngine> engines) {
        super(engines);
        this.service = service;
        this.countryService = countryService;
        this.spexService = spexService;
        this.taskService = taskService;
        this.tagService = tagService;
        this.typeService = typeService;
    }

    @Override
    protected List<ReportHolder<?>> getReports(final List<Long> ids) {
        final Iterable<Spexare> spexareList = service.streamByIds(ids, Sort.by(Sort.Direction.ASC, "firstName"));
        final Map<String, CountryDto> countries = countryService.findAll().stream()
                .collect(Collectors.toMap(CountryDto::getIsoCode, c -> c));

        return List.of(
                ReportHolder.of(
                        toImpexDto(spexareList, SPEXARE_MAPPER::toImpexDto),
                        SpexareImpexDto.class
                ),
                ReportHolder.of(
                        () -> StreamSupport.stream(spexareList.spliterator(), false)
                                .flatMap(spexare -> Optional.ofNullable(spexare.getAddresses())
                                        .orElse(Collections.emptyList())
                                        .stream()
                                        .map(address -> ADDRESS_MAPPER.toImpexDto(spexare, address, countries.get(address.getCountry()), TYPE_MAPPER.toDto(address.getType()))))
                                .iterator(),
                        AddressImpexDto.class
                ),
                ReportHolder.of(
                        () -> StreamSupport.stream(spexareList.spliterator(), false)
                                .flatMap(spexare -> Optional.ofNullable(spexare.getConsents())
                                        .orElse(Collections.emptyList())
                                        .stream()
                                        .map(consent -> CONSENT_MAPPER.toImpexDto(spexare, consent, TYPE_MAPPER.toDto(consent.getType()))))
                                .iterator(),
                        ConsentImpexDto.class
                ),
                ReportHolder.of(
                        () -> StreamSupport.stream(spexareList.spliterator(), false)
                                .flatMap(spexare -> Optional.ofNullable(spexare.getMemberships())
                                        .orElse(Collections.emptyList())
                                        .stream()
                                        .map(membership -> MEMBERSHIP_MAPPER.toImpexDto(spexare, membership, TYPE_MAPPER.toDto(membership.getType()))))
                                .iterator(),
                        MembershipImpexDto.class
                ),
                ReportHolder.of(
                        () -> StreamSupport.stream(spexareList.spliterator(), false)
                                .flatMap(spexare -> Optional.ofNullable(spexare.getTags())
                                        .orElse(Collections.emptySet())
                                        .stream()
                                        .map(tag -> TAGGING_MAPPER.toImpexDto(spexare, TAG_MAPPER.toDto(tag))))
                                .iterator(),
                        TaggingImpexDto.class
                ),
                ReportHolder.of(
                        () -> StreamSupport.stream(spexareList.spliterator(), false)
                                .flatMap(spexare -> Optional.ofNullable(spexare.getToggles())
                                        .orElse(Collections.emptyList())
                                        .stream()
                                        .map(toggle -> TOGGLE_MAPPER.toImpexDto(spexare, toggle, TYPE_MAPPER.toDto(toggle.getType()))))
                                .iterator(),
                        ToggleImpexDto.class
                ),
                ReportHolder.of(
                        () -> StreamSupport.stream(spexareList.spliterator(), false)
                                .flatMap(spexare -> Optional.ofNullable(spexare.getActivities()).orElse(Collections.emptySet()).stream())
                                .flatMap(activity -> {
                                    final SpexActivity spexActivity = activity.getSpexActivity();
                                    final Set<TaskActivity> taskActivities = Optional.ofNullable(activity.getTaskActivities()).orElse(Collections.emptySet());

                                    if (spexActivity == null || taskActivities.isEmpty()) {
                                        return Stream.empty();
                                    }

                                    return taskActivities.stream().flatMap(taskActivity -> {
                                        final Set<Actor> actors = Optional.ofNullable(taskActivity.getActors()).orElse(Collections.emptySet());
                                        if (actors.isEmpty()) {
                                            return Stream.of(ACTIVITY_MAPPER.toImpexDto(activity.getSpexare(), activity, spexActivity, taskActivity, null, null));
                                        }
                                        return actors.stream().map(actor ->
                                                ACTIVITY_MAPPER.toImpexDto(activity.getSpexare(), activity, spexActivity, taskActivity, actor, TYPE_MAPPER.toDto(actor.getVocal()))
                                        );
                                    });
                                }).iterator(),
                        ActivityImpexDto.class
                ),
                ReportHolder.of(
                        toImpexDto(spexService.streamByIds(Collections.emptyList(), Sort.by(Sort.Direction.ASC, "year")), SPEX_MAPPER::toImpexDto),
                        SpexImpexDto.class,
                        true
                ),
                ReportHolder.of(
                        toImpexDto(spexService.streamRevivalsByParentIds(Collections.emptyList(), Sort.by(Sort.Direction.ASC, "year")), SPEX_MAPPER::toRevivalImpexDto),
                        SpexRevivalImpexDto.class,
                        true
                ),
                ReportHolder.of(
                        toImpexDto(taskService.streamByIds(Collections.emptyList(), Sort.by(Sort.Direction.ASC, "name")), TASK_MAPPER::toImpexDto),
                        TaskImpexDto.class,
                        true
                ),
                ReportHolder.of(
                        toImpexDto(tagService.streamByIds(Collections.emptyList(), Sort.by(Sort.Direction.ASC, "name")), TAG_MAPPER::toImpexDto),
                        TagImpexDto.class,
                        true
                ),
                ReportHolder.of(
                        toImpexDto(typeService.streamByIds(Collections.emptyList(), Sort.by(Sort.Direction.ASC, "type")), TYPE_MAPPER::toImpexDto),
                        TypeImpexDto.class,
                        true
                )
        );
    }
}
