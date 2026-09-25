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
import nu.fgv.register.server.impex.importing.AbstractImportService;
import nu.fgv.register.server.impex.importing.ImportEngine;
import nu.fgv.register.server.impex.importing.ImportSpec;
import nu.fgv.register.server.impex.model.HasImpexAction;
import nu.fgv.register.server.impex.model.ImpexAction;
import nu.fgv.register.server.impex.model.ImportResultDto;
import nu.fgv.register.server.settings.CountryService;
import nu.fgv.register.server.settings.TypeService;
import nu.fgv.register.server.spex.SpexService;
import nu.fgv.register.server.spexare.activity.ActivityImpexDto;
import nu.fgv.register.server.spexare.activity.ActivityService;
import nu.fgv.register.server.spexare.activity.spex.SpexActivityService;
import nu.fgv.register.server.spexare.activity.task.TaskActivityService;
import nu.fgv.register.server.spexare.activity.task.actor.ActorService;
import nu.fgv.register.server.spexare.address.AddressImpexDto;
import nu.fgv.register.server.spexare.address.AddressService;
import nu.fgv.register.server.spexare.consent.ConsentImpexDto;
import nu.fgv.register.server.spexare.consent.ConsentService;
import nu.fgv.register.server.spexare.membership.MembershipImpexDto;
import nu.fgv.register.server.spexare.membership.MembershipService;
import nu.fgv.register.server.spexare.tagging.TaggingImpexDto;
import nu.fgv.register.server.spexare.tagging.TaggingService;
import nu.fgv.register.server.spexare.toggle.ToggleImpexDto;
import nu.fgv.register.server.spexare.toggle.ToggleService;
import nu.fgv.register.server.tag.TagService;
import nu.fgv.register.server.task.TaskService;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static nu.fgv.register.server.spexare.SpexareMapper.SPEXARE_MAPPER;
import static nu.fgv.register.server.spexare.activity.task.actor.ActorMapper.ACTOR_MAPPER;
import static nu.fgv.register.server.spexare.address.AddressMapper.ADDRESS_MAPPER;
import static nu.fgv.register.server.spexare.consent.ConsentMapper.CONSENT_MAPPER;
import static nu.fgv.register.server.spexare.membership.MembershipMapper.MEMBERSHIP_MAPPER;
import static nu.fgv.register.server.spexare.toggle.ToggleMapper.TOGGLE_MAPPER;
import static nu.fgv.register.server.util.FileUtil.downloadImage;
import static nu.fgv.register.server.util.FileUtil.isLocalUrl;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author Anders Jacobsson
 * @since 2.0
 */
@Slf4j
@Service
public class SpexareImportService extends AbstractImportService {

    private static final String SPEXARE = "spexare.impex.entityName";

    private final SpexareService service;
    private final AddressService addressService;
    private final ConsentService consentService;
    private final MembershipService membershipService;
    private final ToggleService toggleService;
    private final ActivityService activityService;
    private final SpexActivityService spexActivityService;
    private final TaskActivityService taskActivityService;
    private final ActorService actorService;
    private final TagService tagService;
    private final TaggingService taggingService;
    private final SpexService spexService;
    private final TaskService taskService;
    private final TypeService typeService;
    private final CountryService countryService;
    private final String baseUrl;

    public SpexareImportService(final List<ImportEngine> engines,
                                final SpexareService service,
                                final AddressService addressService,
                                final ConsentService consentService,
                                final MembershipService membershipService,
                                final ToggleService toggleService,
                                final ActivityService activityService,
                                final SpexActivityService spexActivityService,
                                final TaskActivityService taskActivityService,
                                final ActorService actorService,
                                final TagService tagService,
                                final TaggingService taggingService,
                                final SpexService spexService,
                                final TaskService taskService,
                                final TypeService typeService,
                                final CountryService countryService,
                                final MessageSource messageSource,
                                final PlatformTransactionManager transactionManager,
                                @Value("${spexregister.base-url}") final String baseUrl) {
        super(engines, messageSource, transactionManager);
        this.service = service;
        this.addressService = addressService;
        this.consentService = consentService;
        this.membershipService = membershipService;
        this.toggleService = toggleService;
        this.activityService = activityService;
        this.spexActivityService = spexActivityService;
        this.taskActivityService = taskActivityService;
        this.actorService = actorService;
        this.tagService = tagService;
        this.taggingService = taggingService;
        this.spexService = spexService;
        this.taskService = taskService;
        this.typeService = typeService;
        this.countryService = countryService;
        this.baseUrl = baseUrl;
    }

    @Override
    protected List<ImportSpec> getImportSpecs() {
        return List.of(
                ImportSpec.builder()
                        .clazz(SpexareImpexDto.class)
                        .existenceCheckers(Map.of(
                                "id", v -> service.exists((Long) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(AddressImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> addressService.exists((Long) v),
                                "country", v -> countryService.exists((String) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(ConsentImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> consentService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(MembershipImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> membershipService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(TaggingImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "tagId", v -> tagService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(ToggleImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> toggleService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build(),
                ImportSpec.builder()
                        .clazz(ActivityImpexDto.class)
                        .existenceCheckers(Map.of(
                                "spexareId", v -> service.exists((Long) v),
                                "id", v -> activityService.exists((Long) v),
                                "spexActivityId", v -> spexActivityService.exists((Long) v),
                                "spexId", v -> spexService.exists((Long) v),
                                "taskActivityId", v -> taskActivityService.exists((Long) v),
                                "taskId", v -> taskService.exists((Long) v),
                                "actorId", v -> actorService.exists((Long) v),
                                "typeId", v -> typeService.exists((String) v)
                        ))
                        .build()
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ImportResultDto processImport(final Map<Class<?>, List<?>> data, final Locale locale) {
        final ImportSummary summary = new ImportSummary(messageSource, locale);
        final Map<Long, Long> spexareIdMap = new HashMap<>();
        final List<PartnerLink> partnerLinks = new ArrayList<>();

        // Everything belonging to one spexare commits or rolls back together; one bad spexare leaves the rest in place.
        units(data).forEach(unit -> {
            final ImportSummary unitSummary = new ImportSummary(messageSource, locale);
            final Map<Long, Long> unitIdMap = new HashMap<>();
            final List<PartnerLink> unitLinks = new ArrayList<>();

            try {
                inTransaction(() -> importUnit(unit, unitIdMap, unitLinks, unitSummary));
                summary.addAll(unitSummary);
                spexareIdMap.putAll(unitIdMap);
                partnerLinks.addAll(unitLinks);
            } catch (final Exception e) {
                final RowFailure failure = e instanceof final RowFailure f ? f : new RowFailure(unit.firstRowNumber(), SPEXARE, e);

                log.error("Failed to import spexare at row {}", failure.rowNumber, failure.getCause());
                summary.addError(failure.rowNumber, failure.entity,
                        messageSource.getMessage("spexare.impex.rolledBack", new Object[]{failure.getCause().getMessage()}, locale));
            }
        });

        // Partners can be any two spexare in the file, so they are linked once every spexare exists.
        partnerLinks.forEach(link -> {
            try {
                inTransaction(() -> linkPartner(link, spexareIdMap));
            } catch (final Exception e) {
                log.error("Failed to link partner at row {}", link.rowNumber(), e);
                summary.addError(link.rowNumber(), SPEXARE, e.getMessage());
            }
        });

        return summary.toResult();
    }

    @SuppressWarnings("unchecked")
    private static Collection<SpexareUnit> units(final Map<Class<?>, List<?>> data) {
        final Map<Long, SpexareUnit> units = new LinkedHashMap<>();
        final Function<Long, SpexareUnit> unitOf = id -> units.computeIfAbsent(id, _ -> new SpexareUnit());

        rows(data, SpexareImpexDto.class).forEach(dto -> unitOf.apply(dto.getId()).spexare().add(dto));
        rows(data, AddressImpexDto.class).forEach(dto -> unitOf.apply(dto.getSpexareId()).addresses().add(dto));
        rows(data, ConsentImpexDto.class).forEach(dto -> unitOf.apply(dto.getSpexareId()).consents().add(dto));
        rows(data, MembershipImpexDto.class).forEach(dto -> unitOf.apply(dto.getSpexareId()).memberships().add(dto));
        rows(data, ToggleImpexDto.class).forEach(dto -> unitOf.apply(dto.getSpexareId()).toggles().add(dto));
        rows(data, TaggingImpexDto.class).forEach(dto -> unitOf.apply(dto.getSpexareId()).taggings().add(dto));
        rows(data, ActivityImpexDto.class).forEach(dto -> unitOf.apply(dto.getSpexareId()).activities().add(dto));

        return units.values();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> rows(final Map<Class<?>, List<?>> data, final Class<T> type) {
        return Optional.ofNullable((List<T>) data.get(type)).orElseGet(List::of);
    }

    private void importUnit(final SpexareUnit unit, final Map<Long, Long> idMap, final List<PartnerLink> partnerLinks, final ImportSummary summary) {
        final Function<Long, Long> realSpexareId = id -> idMap.getOrDefault(id, id);

        unit.spexare().forEach(dto -> row(dto, SPEXARE, summary, () -> apply(dto,
                d -> {
                    final SpexareDto spexare = service.create(SPEXARE_MAPPER.toCreateDto(d));

                    idMap.put(d.getId(), spexare.getId());
                    processImage(spexare.getId(), d.getImageUrl());

                    if (d.getPartnerId() != null) {
                        partnerLinks.add(new PartnerLink(spexare.getId(), d.getPartnerId(), d.getRowNumber()));
                    }
                },
                d -> {
                    final SpexareDto spexare = service.partialUpdate(SPEXARE_MAPPER.toUpdateDto(d));

                    idMap.put(d.getId(), spexare.getId());
                    processImage(spexare.getId(), d.getImageUrl());

                    if (d.getPartnerId() != null) {
                        partnerLinks.add(new PartnerLink(spexare.getId(), d.getPartnerId(), d.getRowNumber()));
                    } else if (service.findPartnerBySpexare(spexare.getId()).isPresent()) {
                        service.removePartner(spexare.getId());
                    }
                },
                d -> service.deleteById(d.getId())
        )));

        unit.addresses().forEach(dto -> row(dto, "spexare.impex.address.entityName", summary, () -> apply(dto,
                d -> addressService.create(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), ADDRESS_MAPPER.toCreateDto(d)),
                d -> addressService.partialUpdate(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), d.getId(), ADDRESS_MAPPER.toUpdateDto(d)),
                d -> addressService.deleteById(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), d.getId())
        )));

        unit.consents().forEach(dto -> row(dto, "spexare.impex.consent.entityName", summary, () -> apply(dto,
                d -> consentService.create(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), CONSENT_MAPPER.toCreateDto(d)),
                d -> consentService.update(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), d.getId(), CONSENT_MAPPER.toUpdateDto(d)),
                d -> consentService.deleteById(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), d.getId())
        )));

        unit.memberships().forEach(dto -> row(dto, "spexare.impex.membership.entityName", summary, () -> apply(dto,
                d -> membershipService.create(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), MEMBERSHIP_MAPPER.toCreateDto(d)),
                d -> { /* Not applicable */ },
                d -> membershipService.deleteById(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), d.getId())
        )));

        unit.toggles().forEach(dto -> row(dto, "spexare.impex.toggle.entityName", summary, () -> apply(dto,
                d -> toggleService.create(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), TOGGLE_MAPPER.toCreateDto(d)),
                d -> toggleService.update(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), d.getId(), TOGGLE_MAPPER.toUpdateDto(d)),
                d -> toggleService.deleteById(realSpexareId.apply(d.getSpexareId()), d.getTypeId(), d.getId())
        )));

        unit.taggings().forEach(dto -> row(dto, "spexare.impex.tagging.entityName", summary, () -> apply(dto,
                d -> taggingService.create(realSpexareId.apply(d.getSpexareId()), d.getTagId()),
                d -> { /* Not applicable */ },
                d -> taggingService.deleteById(realSpexareId.apply(d.getSpexareId()), d.getTagId())
        )));

        importActivities(unit.activities(), realSpexareId, summary);
    }

    private static void row(final HasImpexAction dto, final String entity, final ImportSummary summary, final Runnable work) {
        try {
            work.run();
        } catch (final RuntimeException e) {
            throw new RowFailure(dto.getRowNumber(), entity, e);
        }
        summary.increment(dto.getAction(), entity);
    }

    private void linkPartner(final PartnerLink link, final Map<Long, Long> spexareIdMap) {
        final Long realPartnerId = spexareIdMap.getOrDefault(link.partnerId(), link.partnerId());
        final SpexareDto currentPartner = service.findPartnerBySpexare(link.spexareId()).orElse(null);

        if (currentPartner == null || !currentPartner.getId().equals(realPartnerId)) {
            service.addPartner(link.spexareId(), realPartnerId);
        }
    }

    private void importActivities(final List<ActivityImpexDto> dtos,
                                  final Function<Long, Long> realSpexareIdOf,
                                  final ImportSummary summary) {
        final Map<Long, Long> activityIdMap = new HashMap<>();
        final Map<Long, Long> spexActivityIdMap = new HashMap<>();
        final Map<Long, Long> taskActivityIdMap = new HashMap<>();

        dtos.forEach(dto -> {
            try {
                importActivity(dto, realSpexareIdOf.apply(dto.getSpexareId()), activityIdMap, spexActivityIdMap, taskActivityIdMap, summary);
            } catch (final RuntimeException e) {
                throw new RowFailure(dto.getRowNumber(), "activity.impex.entityName", e);
            }
        });
    }

    private void importActivity(final ActivityImpexDto dto,
                                final Long realSpexareId,
                                final Map<Long, Long> activityIdMap,
                                final Map<Long, Long> spexActivityIdMap,
                                final Map<Long, Long> taskActivityIdMap,
                                final ImportSummary summary) {
        Long realActivityId = activityIdMap.get(dto.getId());

        if (realActivityId == null) {
            if (dto.getAction() == ImpexAction.CREATE) {
                realActivityId = activityService.create(realSpexareId).getId();
                summary.incrementCreated("activity.impex.entityName");
            } else if (dto.getAction() == ImpexAction.DELETE) {
                activityService.deleteById(realSpexareId, dto.getId());
                summary.incrementDeleted("activity.impex.entityName");
                return;
            } else {
                realActivityId = dto.getId();
            }
            activityIdMap.put(dto.getId(), realActivityId);
        }

        Long realSpexActivityId = spexActivityIdMap.get(dto.getSpexActivityId());

        if (realSpexActivityId == null) {
            if (dto.getSpexActivityAction() == ImpexAction.CREATE) {
                realSpexActivityId = spexActivityService.create(realSpexareId, realActivityId, dto.getSpexId()).getId();
                summary.incrementCreated("activity.impex.spexActivity.entityName");
            } else if (dto.getSpexActivityAction() == ImpexAction.UPDATE) {
                realSpexActivityId = spexActivityService.update(realSpexareId, realActivityId, dto.getSpexId(), dto.getSpexActivityId()).getId();
                summary.incrementUpdated("activity.impex.spexActivity.entityName");
            } else if (dto.getSpexActivityAction() == ImpexAction.DELETE) {
                spexActivityService.deleteById(realSpexareId, realActivityId, dto.getSpexActivityId());
                summary.incrementDeleted("activity.impex.spexActivity.entityName");
            } else {
                realSpexActivityId = dto.getSpexActivityId();
            }
            if (realSpexActivityId != null) {
                spexActivityIdMap.put(dto.getSpexActivityId(), realSpexActivityId);
            }
        }

        Long realTaskActivityId = taskActivityIdMap.get(dto.getTaskActivityId());

        if (realTaskActivityId == null) {
            if (dto.getTaskActivityAction() == ImpexAction.CREATE) {
                realTaskActivityId = taskActivityService.create(realSpexareId, realActivityId, dto.getTaskId()).getId();
                summary.incrementCreated("activity.impex.taskActivity.entityName");
            } else if (dto.getTaskActivityAction() == ImpexAction.DELETE) {
                taskActivityService.deleteById(realSpexareId, realActivityId, dto.getTaskActivityId());
                summary.incrementDeleted("activity.impex.taskActivity.entityName");
                return;
            } else {
                realTaskActivityId = dto.getTaskActivityId();
            }
            if (realTaskActivityId != null) {
                taskActivityIdMap.put(dto.getTaskActivityId(), realTaskActivityId);
            }
        }

        if (dto.getActorAction() != null) {
            if (dto.getActorAction() == ImpexAction.CREATE) {
                actorService.create(realSpexareId, realActivityId, realTaskActivityId, dto.getTypeId(), ACTOR_MAPPER.toCreateDto(dto));
                summary.incrementCreated("activity.impex.taskActivity.actor.entityName");
            } else if (dto.getActorAction() == ImpexAction.UPDATE) {
                actorService.partialUpdate(realSpexareId, realActivityId, realTaskActivityId, dto.getTypeId(), dto.getActorId(), ACTOR_MAPPER.toUpdateDto(dto));
                summary.incrementUpdated("activity.impex.taskActivity.actor.entityName");
            } else if (dto.getActorAction() == ImpexAction.DELETE) {
                actorService.deleteById(realSpexareId, realActivityId, realTaskActivityId, dto.getTypeId(), dto.getActorId());
                summary.incrementDeleted("activity.impex.taskActivity.actor.entityName");
            }
        }
    }

    private void processImage(final Long id, @Nullable final String url) {
        if (hasText(url) && !isLocalUrl(url, baseUrl)) {
            service.saveImage(id, downloadImage(url));
        } else if (!hasText(url)) {
            service.deleteImage(id);
        }
    }

    private record PartnerLink(Long spexareId, Long partnerId, @Nullable Integer rowNumber) {
    }

    private record SpexareUnit(List<SpexareImpexDto> spexare,
                               List<AddressImpexDto> addresses,
                               List<ConsentImpexDto> consents,
                               List<MembershipImpexDto> memberships,
                               List<ToggleImpexDto> toggles,
                               List<TaggingImpexDto> taggings,
                               List<ActivityImpexDto> activities) {

        SpexareUnit() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        @Nullable Integer firstRowNumber() {
            return Stream.of(spexare, addresses, consents, memberships, toggles, taggings, activities)
                    .flatMap(List::stream)
                    .map(HasImpexAction::getRowNumber)
                    .filter(Objects::nonNull)
                    .min(Integer::compare)
                    .orElse(null);
        }
    }

    private static final class RowFailure extends RuntimeException {
        private final @Nullable Integer rowNumber;
        private final String entity;

        RowFailure(final @Nullable Integer rowNumber, final String entity, final Throwable cause) {
            super(cause.getMessage(), cause);
            this.rowNumber = rowNumber;
            this.entity = entity;
        }
    }
}
