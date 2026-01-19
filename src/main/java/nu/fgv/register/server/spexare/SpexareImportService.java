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
import nu.fgv.register.server.impex.importing.AbstractImportService;
import nu.fgv.register.server.impex.importing.ImportEngine;
import nu.fgv.register.server.impex.importing.ImportSpec;
import nu.fgv.register.server.impex.model.ImpexAction;
import nu.fgv.register.server.impex.model.ImportResultDto;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                                @Value("${spexregister.base-url}") final String baseUrl) {
        super(engines, messageSource);
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

    @SuppressWarnings("unchecked")
    @Override
    protected ImportResultDto processImport(final Map<Class<?>, List<?>> data, final Locale locale) {
        final Map<Long, Long> spexareIdMap = new HashMap<>();
        final List<Pair<Long, Long>> partnerLinks = new ArrayList<>();
        final ImportSummary summary = new ImportSummary(messageSource, locale);

        handleImport(
                (List<SpexareImpexDto>) data.get(SpexareImpexDto.class),
                summary,
                "spexare.impex.entityName",
                dto -> {
                    final SpexareDto spexare = service.create(SPEXARE_MAPPER.toCreateDto(dto));

                    spexareIdMap.put(dto.getId(), spexare.getId());
                    processImage(spexare.getId(), dto.getImageUrl());

                    if (dto.getPartnerId() != null) {
                        partnerLinks.add(Pair.of(spexare.getId(), dto.getPartnerId()));
                    }
                },
                dto -> {
                    final SpexareDto spexare = service.partialUpdate(SPEXARE_MAPPER.toUpdateDto(dto));

                    spexareIdMap.put(dto.getId(), spexare.getId());
                    processImage(spexare.getId(), dto.getImageUrl());

                    final SpexareDto currentPartner = service.findPartnerBySpexare(spexare.getId()).orElse(null);

                    if (dto.getPartnerId() != null) {
                        partnerLinks.add(Pair.of(spexare.getId(), dto.getPartnerId()));
                    } else if (currentPartner != null) {
                        service.removePartner(spexare.getId());
                    }
                },
                dto -> service.deleteById(dto.getId())
        );

        partnerLinks.forEach(link -> {
            final Long spexareId = link.getLeft();
            final Long partnerId = link.getRight();
            final Long realPartnerId = spexareIdMap.getOrDefault(partnerId, partnerId);
            final SpexareDto currentPartner = service.findPartnerBySpexare(spexareId).orElse(null);

            if (currentPartner == null || !currentPartner.getId().equals(realPartnerId)) {
                service.addPartner(spexareId, realPartnerId);
            }
        });

        handleImport(
                (List<AddressImpexDto>) data.get(AddressImpexDto.class),
                summary,
                "spexare.impex.address.entityName",
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    addressService.create(realSpexareId, dto.getTypeId(), ADDRESS_MAPPER.toCreateDto(dto));
                },
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    addressService.partialUpdate(realSpexareId, dto.getTypeId(), dto.getId(), ADDRESS_MAPPER.toUpdateDto(dto));
                },
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    addressService.deleteById(realSpexareId, dto.getTypeId(), dto.getId());
                }
        );

        handleImport(
                (List<ConsentImpexDto>) data.get(ConsentImpexDto.class),
                summary,
                "spexare.impex.consent.entityName",
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    consentService.create(realSpexareId, dto.getTypeId(), CONSENT_MAPPER.toCreateDto(dto));
                },
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    consentService.update(realSpexareId, dto.getTypeId(), dto.getId(), CONSENT_MAPPER.toUpdateDto(dto));
                },
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    consentService.deleteById(realSpexareId, dto.getTypeId(), dto.getId());
                }
        );

        handleImport(
                (List<MembershipImpexDto>) data.get(MembershipImpexDto.class),
                summary,
                "spexare.impex.membership.entityName",
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    membershipService.create(realSpexareId, dto.getTypeId(), MEMBERSHIP_MAPPER.toCreateDto(dto));
                },
                dto -> { /* Not applicable */ },
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    membershipService.deleteById(realSpexareId, dto.getTypeId(), dto.getId());
                }
        );

        handleImport(
                (List<ToggleImpexDto>) data.get(ToggleImpexDto.class),
                summary,
                "spexare.impex.toggle.entityName",
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    toggleService.create(realSpexareId, dto.getTypeId(), TOGGLE_MAPPER.toCreateDto(dto));
                },
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    toggleService.update(realSpexareId, dto.getTypeId(), dto.getId(), TOGGLE_MAPPER.toUpdateDto(dto));
                },
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    toggleService.deleteById(realSpexareId, dto.getTypeId(), dto.getId());
                }
        );

        handleImport(
                (List<TaggingImpexDto>) data.get(TaggingImpexDto.class),
                summary,
                "spexare.impex.tagging.entityName",
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    taggingService.create(realSpexareId, dto.getTagId());
                },
                dto -> { /* Not applicable */ },
                dto -> {
                    final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

                    taggingService.deleteById(realSpexareId, dto.getTagId());
                }
        );

        handleActivities((List<ActivityImpexDto>) data.get(ActivityImpexDto.class), spexareIdMap, summary);

        return summary.toResult();
    }

    private void handleActivities(@Nullable final List<ActivityImpexDto> dtos,
                                  final Map<Long, Long> spexareIdMap,
                                  final ImportSummary summary) {
        if (dtos == null) {
            return;
        }

        final Map<Long, Long> activityIdMap = new HashMap<>();
        final Map<Long, Long> spexActivityIdMap = new HashMap<>();
        final Map<Long, Long> taskActivityIdMap = new HashMap<>();

        dtos.forEach(dto -> {
            try {
                final Long realSpexareId = spexareIdMap.getOrDefault(dto.getSpexareId(), dto.getSpexareId());

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
            } catch (final Exception e) {
                log.error("Error processing activity", e);
                summary.addError(dto.getRowNumber(), "activity.impex.entityName", e.getMessage());
            }
        });
    }

    private void processImage(final Long id, @Nullable final String url) {
        if (hasText(url) && !isLocalUrl(url, baseUrl)) {
            service.saveImage(id, downloadImage(url), null);
        } else if (!hasText(url)) {
            service.deleteImage(id);
        }
    }
}
